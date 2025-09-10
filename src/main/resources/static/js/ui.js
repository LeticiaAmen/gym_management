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

if (document.querySelector('#clients-table')) {
    document.addEventListener('DOMContentLoaded', renderClientsTable);
}
