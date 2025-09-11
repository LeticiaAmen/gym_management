/*
 * Es importante cargar este archivo como módulo (type="module" en el HTML)
 * porque así se habilita el uso de import/export entre archivos JS.
 * Esto permite separar la lógica en distintos archivos y reutilizar funciones,
 * como apiFetch, manteniendo el código más organizado y seguro.
 * Los scripts clásicos no permiten imports y pueden causar errores de dependencias.
 */
// Renderiza la tabla de clientes en el dashboard
import { apiFetch } from './api.js';

async function renderClientsTable() {
    try {
        const res = await apiFetch('/clients');
        if (!res.ok) throw new Error('No se pudo cargar la lista de clientes');
        const clientes = await res.json();
        console.log(clientes); // Depuración: ver qué llega realmente
        const tbody = document.querySelector('#clients-table tbody');
        tbody.innerHTML = '';
        if (!Array.isArray(clientes) || clientes.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6">No hay clientes registrados.</td></tr>';
            return;
        }
        clientes.forEach(c => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
        <td>${c.id}</td>
        <td>${c.firstName}</td>
        <td>${c.lastName}</td>
        <td>${c.user?.email || ''}</td>
        <td>${c.telephone || ''}</td>
        <td>${c.isActive ? 'Activo' : 'Inactivo'}</td>
      `;
            tbody.appendChild(tr);
        });
    } catch {
        const tbody = document.querySelector('#clients-table tbody');
        tbody.innerHTML = '<tr><td colspan="6">No se pudo cargar la lista de clientes.</td></tr>';
    }
}

// Manejo de secciones
function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    document.getElementById(`${sectionId}-section`).style.display = 'block';
}

// Gestión de Clientes
async function loadClients() {
    try {
        const response = await apiFetch('/clients');
        const clients = await response.json();
        const tbody = document.querySelector('#clients-table tbody');
        tbody.innerHTML = clients.map(client => `
            <tr>
                <td>${client.id}</td>
                <td>${client.firstName}</td>
                <td>${client.lastName}</td>
                <td>${client.email}</td>
                <td>${client.phone || '-'}</td>
                <td>${client.active ? 'Activo' : 'Inactivo'}</td>
                <td>${new Date(client.startDate).toLocaleDateString()}</td>
                <td>
                    <button onclick="editClient(${client.id})" class="btn">Editar</button>
                    ${client.active ? 
                        `<button onclick="deactivateClient(${client.id})" class="btn btn-danger">Desactivar</button>` :
                        `<button onclick="activateClient(${client.id})" class="btn btn-primary">Activar</button>`
                    }
                    <button onclick="showPauseForm(${client.id})" class="btn">Pausar</button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Error al cargar clientes:', error);
    }
}

// Gestión de Pagos
async function loadPayments(filters = {}) {
    try {
        const queryParams = new URLSearchParams(filters);
        const response = await apiFetch('/payments?' + queryParams);
        const payments = await response.json();
        const tbody = document.querySelector('#payments-table tbody');
        tbody.innerHTML = payments.map(payment => `
            <tr>
                <td>${payment.id}</td>
                <td>${payment.clientId}</td>
                <td>$${payment.amount.toFixed(2)}</td>
                <td>${payment.method}</td>
                <td>${new Date(payment.paymentDate).toLocaleDateString()}</td>
                <td>${payment.state}</td>
                <td>
                    ${!payment.voided ? 
                        `<button onclick="voidPayment(${payment.id})" class="btn btn-danger">Anular</button>` :
                        '<span>Anulado</span>'
                    }
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Error al cargar pagos:', error);
    }
}

// Reportes
async function getExpiringReport() {
    try {
        const response = await apiFetch('/reports/expiring');
        const clients = await response.json();
        showReportResults('Pagos por Vencer (7 días)', clients);
    } catch (error) {
        console.error('Error al cargar reporte:', error);
    }
}

async function getOverdueReport() {
    try {
        const response = await apiFetch('/reports/overdue');
        const clients = await response.json();
        showReportResults('Pagos Vencidos', clients);
    } catch (error) {
        console.error('Error al cargar reporte:', error);
    }
}

async function getCashflowReport() {
    const from = document.getElementById('cashflow-from').value;
    const to = document.getElementById('cashflow-to').value;
    if (!from || !to) {
        alert('Por favor seleccione un rango de fechas');
        return;
    }
    try {
        const response = await apiFetch(`/reports/cashflow?from=${from}&to=${to}`);
        const total = await response.json();
        showReportResults('Flujo de Caja', `Total: $${total.toFixed(2)}`);
    } catch (error) {
        console.error('Error al cargar reporte:', error);
    }
}

function showReportResults(title, data) {
    const resultsDiv = document.getElementById('report-results');
    resultsDiv.innerHTML = `
        <h3>${title}</h3>
        ${typeof data === 'string' ? data : 
            `<table class="table">
                <thead>
                    <tr>
                        <th>Cliente</th>
                        <th>Email</th>
                        <th>Estado</th>
                    </tr>
                </thead>
                <tbody>
                    ${data.map(client => `
                        <tr>
                            <td>${client.firstName} ${client.lastName}</td>
                            <td>${client.email}</td>
                            <td>${client.active ? 'Activo' : 'Inactivo'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>`
        }
    `;
}

// Event Listeners y carga inicial
document.addEventListener('DOMContentLoaded', () => {
    loadClients();
    // Asignar eventos a filtros de pagos
    document.getElementById('payment-state').addEventListener('change', filterPayments);
});

if (document.querySelector('#clients-table')) {
    document.addEventListener('DOMContentLoaded', renderClientsTable);
}

// Exponer funciones globalmente
window.showSection = showSection;
window.getExpiringReport = getExpiringReport;
window.getOverdueReport = getOverdueReport;
window.getCashflowReport = getCashflowReport;
