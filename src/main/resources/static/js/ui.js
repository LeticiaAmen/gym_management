/*
 * Es importante cargar este archivo como módulo (type="module" en el HTML)
 * porque así se habilita el uso de import/export entre archivos JS.
 * Esto permite separar la lógica en distintos archivos y reutilizar funciones,
 * como apiFetch, manteniendo el código más organizado y seguro.
 * Los scripts clásicos no permiten imports y pueden causar errores de dependencias.
 */
import { apiFetch } from './api.js';

// Helper: normaliza fechas a yyyy-MM-dd para inputs type=date
function formatDateForInput(value) {
    if (!value) return '';
    // Si ya viene en yyyy-MM-dd, devolver tal cual
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return '';
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
}

// Determina si el cliente está pausado actualmente (hoy ∈ [pausedFrom, pausedTo])
function isCurrentlyPaused(client) {
    if (!client || !client.pausedFrom || !client.pausedTo) return false;
    const today = new Date();
    const from = new Date(client.pausedFrom);
    const to = new Date(client.pausedTo);
    // normalizar a medianoche
    from.setHours(0,0,0,0);
    to.setHours(0,0,0,0);
    today.setHours(0,0,0,0);
    return today >= from && today <= to;
}

// Cache simple en memoria para reutilizar datos en acciones (editar)
let clientsCache = [];

// Navegación simple entre secciones
function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    const sec = document.getElementById(`${sectionId}-section`);
    if (sec) sec.style.display = 'block';
    if (sectionId === 'clients') loadClients();
    if (sectionId === 'payments') loadPayments();
}

function logout() {
    localStorage.removeItem('token');
    window.location.href = '/index.html';
}

// ============= Clientes =============
async function loadClients() {
    try {
        const response = await apiFetch('/api/clients');
        if (!response.ok) throw new Error('No se pudo cargar la lista de clientes');
        const clients = await response.json();
        clientsCache = Array.isArray(clients) ? clients : []; // actualizar caché
        const tbody = document.querySelector('#clients-table tbody');
        if (!tbody) return;
        if (!Array.isArray(clients) || clients.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8">No hay clientes registrados.</td></tr>';
            return;
        }
        tbody.innerHTML = clients.map(client => {
            const start = client.startDate ? new Date(client.startDate).toLocaleDateString() : '-';
            const pauseBtn = isCurrentlyPaused(client)
                ? `<button type="button" class="btn js-resume" data-id="${client.id}">Reanudar</button>`
                : `<button type="button" class="btn js-pause" data-id="${client.id}">Pausar</button>`;
            return `
            <tr>
                <td>${client.id}</td>
                <td>${client.firstName || ''}</td>
                <td>${client.lastName || ''}</td>
                <td>${client.email || ''}</td>
                <td>${client.phone || '-'}</td>
                <td>${client.active ? 'Activo' : 'Inactivo'}</td>
                <td>${start}</td>
                <td>
                    <button type="button" class="btn js-edit" data-id="${client.id}">Editar</button>
                    ${client.active ? 
                        `<button type="button" class="btn btn-danger js-deactivate" data-id="${client.id}">Desactivar</button>` :
                        `<button type="button" class="btn btn-primary js-activate" data-id="${client.id}">Activar</button>`
                    }
                    ${pauseBtn}
                </td>
            </tr>`;
        }).join('');
    } catch (error) {
        console.error('Error al cargar clientes:', error);
        const tbody = document.querySelector('#clients-table tbody');
        if (tbody) tbody.innerHTML = '<tr><td colspan="8">No se pudo cargar la lista de clientes.</td></tr>';
    }
}

// Stubs de acciones de clientes (TODO: implementar)
async function showClientForm() {
    const modal = document.getElementById('client-modal');
    if (!modal) return;
    // Cargar plantilla del formulario
    const tplRes = await fetch('/admin/templates/client-form.html');
    const html = await tplRes.text();
    modal.innerHTML = html;
    modal.style.display = 'flex'; // centrado con flex
    document.body.style.overflow = 'hidden'; // bloquear scroll del fondo

    // Cerrar al hacer click fuera
    modal.onclick = (e) => { if (e.target === modal) closeModal('client-modal'); };

    // Manejar submit
    const form = modal.querySelector('#client-form');
    if (!form) return;
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            firstName: form.firstName.value.trim(),
            lastName: form.lastName.value.trim(),
            email: form.email.value.trim(),
            phone: form.phone.value.trim() || null,
            startDate: form.startDate.value, // YYYY-MM-DD
            notes: form.notes.value.trim() || null
        };
        const errEl = document.getElementById('client-form-error');
        if (errEl) errEl.textContent = '';
        try {
            const res = await apiFetch('/api/clients', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
                noRedirectOn401: true
            });
            if (res.status === 401) {
                if (errEl) errEl.textContent = 'Sesión expirada. Inicia sesión nuevamente para continuar.';
                return;
            }
            if (!res.ok) {
                const msg = (await res.text()) || '';
                const duplicate = res.status === 409 || /duplicate|unique|exists|ya existe|constraint/i.test(msg);
                if (errEl) {
                    errEl.textContent = duplicate ? 'El mail ya está registrado' : (msg || 'No se pudo guardar el cliente');
                } else {
                    alert(duplicate ? 'El mail ya está registrado' : (msg || 'No se pudo guardar el cliente'));
                }
                if (duplicate) form.email.focus();
                return;
            }
            closeModal('client-modal');
            await loadClients();
        } catch (err) {
            if (errEl) errEl.textContent = 'Error de red'; else alert('Error de red');
        }
    });
}

function closeModal(id) {
    const modal = document.getElementById(id);
    if (!modal) return;
    modal.style.display = 'none';
    modal.innerHTML = '';
    document.body.style.overflow = ''; // restaurar scroll del fondo
}

function editClient(id) {
    const client = clientsCache.find(c => String(c.id) === String(id));
    if (!client) {
        alert('No se encontró el cliente en memoria. Actualiza la lista e inténtalo de nuevo.');
        return;
    }
    const modal = document.getElementById('client-modal');
    if (!modal) return;
    // Cargar plantilla y prellenar
    fetch('/admin/templates/client-form.html')
        .then(r => r.text())
        .then(html => {
            modal.innerHTML = html;
            modal.style.display = 'flex';
            document.body.style.overflow = 'hidden';

            const form = modal.querySelector('#client-form');
            const title = modal.querySelector('.modal-content h3');
            if (title) title.textContent = 'Editar Cliente';

            // Prellenar campos
            if (form) {
                form.firstName.value = client.firstName || '';
                form.lastName.value = client.lastName || '';
                form.email.value = client.email || '';
                form.phone.value = client.phone || '';
                form.startDate.value = formatDateForInput(client.startDate);
                form.notes.value = client.notes || '';

                form.addEventListener('submit', async (e) => {
                    e.preventDefault();
                    const payload = {
                        firstName: form.firstName.value.trim(),
                        lastName: form.lastName.value.trim(),
                        email: form.email.value.trim(),
                        phone: form.phone.value.trim() || null,
                        startDate: form.startDate.value,
                        notes: form.notes.value.trim() || null
                    };
                    const errEl = document.getElementById('client-form-error');
                    if (errEl) errEl.textContent = '';
                    try {
                        const res = await apiFetch(`/api/clients/${id}`, {
                            method: 'PUT',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(payload),
                            noRedirectOn401: true
                        });
                        if (res.status === 401) {
                            if (errEl) errEl.textContent = 'Sesión expirada. Inicia sesión nuevamente para continuar.';
                            return;
                        }
                        if (!res.ok) {
                            const msg = (await res.text()) || '';
                            const duplicate = res.status === 409 || /duplicate|unique|exists|ya existe|constraint/i.test(msg);
                            if (errEl) {
                                errEl.textContent = duplicate ? 'El mail ya está registrado' : (msg || 'No se pudo actualizar el cliente');
                            } else {
                                alert(duplicate ? 'El mail ya está registrado' : (msg || 'No se pudo actualizar el cliente'));
                            }
                            if (duplicate) form.email.focus();
                            return;
                        }
                        closeModal('client-modal');
                        await loadClients();
                    } catch (err) {
                        if (errEl) errEl.textContent = 'Error de red'; else alert('Error de red');
                    }
                });
            }

            // Cerrar al clickear fuera del contenido
            modal.onclick = (e) => { if (e.target === modal) closeModal('client-modal'); };
        });
}

// Delegación de eventos en la tabla de clientes
function onClientsTableClick(e) {
    const btn = e.target.closest('button');
    if (!btn) return;
    const id = btn.getAttribute('data-id');
    if (btn.classList.contains('js-edit')) {
        editClient(Number(id));
        return;
    }
    if (btn.classList.contains('js-deactivate')) {
        if (!confirm('¿Confirmás desactivar este cliente?')) return;
        const prev = btn.textContent; btn.disabled = true; btn.textContent = 'Desactivando…';
        apiFetch(`/api/clients/${id}`, { method: 'DELETE', noRedirectOn401: true })
          .then(res => {
            if (res.status === 401) { alert('Sesión expirada. Inicia sesión nuevamente para continuar.'); return; }
            if (!res.ok) return res.text().then(t => alert(t || 'No se pudo desactivar el cliente'));
            alert('Cliente desactivado');
            loadClients();
          })
          .catch(() => alert('Error de red'))
          .finally(() => { btn.disabled = false; btn.textContent = prev; });
        return;
    }
    if (btn.classList.contains('js-activate')) {
        if (!confirm('¿Confirmás activar este cliente?')) return;
        const prev = btn.textContent; btn.disabled = true; btn.textContent = 'Activando…';
        apiFetch(`/api/clients/${id}/activate`, { method: 'POST', noRedirectOn401: true })
          .then(res => {
            if (res.status === 401) { alert('Sesión expirada. Inicia sesión nuevamente para continuar.'); return; }
            if (!res.ok) return res.text().then(t => alert(t || 'No se pudo activar el cliente'));
            alert('Cliente activado');
            loadClients();
          })
          .catch(() => alert('Error de red'))
          .finally(() => { btn.disabled = false; btn.textContent = prev; });
        return;
    }
    if (btn.classList.contains('js-pause')) {
        showPauseForm(Number(id));
        return;
    }
    if (btn.classList.contains('js-resume')) {
        if (!confirm('¿Confirmás reanudar la suscripción de este cliente?')) return;
        const prev = btn.textContent; btn.disabled = true; btn.textContent = 'Reanudando…';
        apiFetch(`/api/clients/${id}/resume`, { method: 'POST', noRedirectOn401: true })
          .then(res => {
            if (res.status === 401) { alert('Sesión expirada. Inicia sesión nuevamente para continuar.'); return; }
            if (!res.ok) return res.text().then(t => alert(t || 'No se pudo reanudar la suscripción'));
            alert('Suscripción reanudada');
            loadClients();
          })
          .catch(() => alert('Error de red'))
          .finally(() => { btn.disabled = false; btn.textContent = prev; });
        return;
    }
}

// Mostrar modal de pausa y enviar solicitud
function showPauseForm(clientId) {
    const modal = document.getElementById('pause-modal');
    if (!modal) return;
    fetch('/admin/templates/pause-form.html')
      .then(r => r.text())
      .then(html => {
        modal.innerHTML = html;
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
        modal.onclick = (e) => { if (e.target === modal) closeModal('pause-modal'); };

        const form = modal.querySelector('#pause-form');
        const errEl = modal.querySelector('#pause-form-error');
        modal.querySelector('#pause-client-id').value = clientId;

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (errEl) errEl.textContent = '';
            const from = form.querySelector('#pauseFrom').value;
            const to = form.querySelector('#pauseTo').value;
            const reason = form.querySelector('#pauseReason').value.trim();
            if (!from || !to) { if (errEl) errEl.textContent = 'Debe completar fechas desde y hasta'; return; }
            if (new Date(to) < new Date(from)) { if (errEl) errEl.textContent = 'La fecha "Hasta" no puede ser anterior a "Desde"'; return; }
            const btn = form.querySelector('button[type="submit"]');
            const prev = btn.textContent; btn.disabled = true; btn.textContent = 'Pausando…';
            try {
                const qs = new URLSearchParams({ from, to });
                if (reason) qs.append('reason', reason);
                const res = await apiFetch(`/api/clients/${clientId}/pause?${qs.toString()}`, { method: 'POST', noRedirectOn401: true });
                if (res.status === 401) { if (errEl) errEl.textContent = 'Sesión expirada. Inicia sesión nuevamente para continuar.'; return; }
                if (!res.ok) { const msg = await res.text(); if (errEl) errEl.textContent = msg || 'No se pudo pausar la suscripción'; return; }
                closeModal('pause-modal');
                loadClients();
                alert('Suscripción pausada');
            } catch {
                if (errEl) errEl.textContent = 'Error de red';
            } finally {
                btn.disabled = false; btn.textContent = prev;
            }
        });
      });
}

// ============= Pagos =============
async function loadPayments(filters = {}) {
    try {
        const queryParams = new URLSearchParams(filters);
        const response = await apiFetch('/api/payments?' + queryParams);
        if (!response.ok) throw new Error('No se pudo cargar pagos');
        const payments = await response.json();
        const tbody = document.querySelector('#payments-table tbody');
        if (!tbody) return;
        if (!Array.isArray(payments) || payments.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7">Sin resultados.</td></tr>';
            return;
        }
        tbody.innerHTML = payments.map(payment => `
            <tr>
                <td>${payment.id}</td>
                <td>${payment.clientId}</td>
                <td>$${Number(payment.amount || 0).toFixed(2)}</td>
                <td>${payment.method || '-'}</td>
                <td>${payment.paymentDate ? new Date(payment.paymentDate).toLocaleDateString() : '-'}</td>
                <td>${payment.state || '-'}</td>
                <td>
                    ${!payment.voided ? 
                        `<button onclick="voidPayment(${payment.id})" class="btn btn-danger">Anular</button>` :
                        '<span>Anulado</span>'
                    }
                </td>
            </tr>`).join('');
    } catch (error) {
        console.error('Error al cargar pagos:', error);
    }
}

function filterPayments() {
    const from = document.getElementById('date-from')?.value;
    const to = document.getElementById('date-to')?.value;
    const state = document.getElementById('payment-state')?.value;
    const filters = {};
    if (from) filters.from = from;
    if (to) filters.to = to;
    if (state) filters.state = state;
    loadPayments(filters);
}

function voidPayment(id) { /* TODO */ }
function showPaymentForm() { /* TODO */ }

// ============= Reportes =============
async function getExpiringReport() {
    try {
        const response = await apiFetch('/api/reports/expiring');
        const clients = await response.json();
        showReportResults('Pagos por Vencer (7 días)', clients);
    } catch (error) {
        console.error('Error al cargar reporte:', error);
    }
}

async function getOverdueReport() {
    try {
        const response = await apiFetch('/api/reports/overdue');
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
        const response = await apiFetch(`/api/reports/cashflow?from=${from}&to=${to}`);
        const total = await response.json();
        showReportResults('Flujo de Caja', `Total: $${Number(total || 0).toFixed(2)}`);
    } catch (error) {
        console.error('Error al cargar reporte:', error);
    }
}

function showReportResults(title, data) {
    const resultsDiv = document.getElementById('report-results');
    if (!resultsDiv) return;
    resultsDiv.innerHTML = `
        <h3>${title}</h3>
        ${typeof data === 'string' ? data : 
            `<div class="table-wrapper"><table class="table">
                <thead>
                    <tr>
                        <th>Cliente</th>
                        <th>Email</th>
                        <th>Estado</th>
                    </tr>
                </thead>
                <tbody>
                    ${Array.isArray(data) ? data.map(client => `
                        <tr>
                            <td>${(client.firstName||'') + ' ' + (client.lastName||'')}</td>
                            <td>${client.email||''}</td>
                            <td>${client.active ? 'Activo' : 'Inactivo'}</td>
                        </tr>
                    `).join('') : ''}
                </tbody>
            </table></div>`
        }
    `;
}

// ============= Inicio =============
document.addEventListener('DOMContentLoaded', () => {
    // Si no hay token, la API devolverá 401 y apiFetch redirigirá a /index.html
    loadClients();
    const stateSel = document.getElementById('payment-state');
    if (stateSel) stateSel.addEventListener('change', filterPayments);
    const clientsTable = document.getElementById('clients-table');
    if (clientsTable) clientsTable.addEventListener('click', onClientsTableClick);
});

// Exponer funciones globales usadas por el HTML (solo las necesarias desde HTML)
window.showSection = showSection;
window.logout = logout;
window.getExpiringReport = getExpiringReport;
window.getOverdueReport = getOverdueReport;
window.getCashflowReport = getCashflowReport;
window.filterPayments = filterPayments;
window.showClientForm = showClientForm;
window.closeModal = closeModal;
