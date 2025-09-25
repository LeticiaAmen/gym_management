/*
 * Es importante cargar este archivo como módulo (type="module" en el HTML)
 * porque así se habilita el uso de import/export entre archivos JS.
 * Esto permite separar la lógica en distintos archivos y reutilizar funciones,
 * como apiFetch, manteniendo el código más organizado y seguro.
 * Los scripts clásicos no permiten imports y pueden causar errores de dependencias.
 */
import { apiFetch } from './api.js';

/* ============================================================================
 * UI / Frontend Admin Panel
 * ============================================================================
 * Este archivo concentra la lógica de interacción del panel administrativo.
 *
 * Estructura general:
 *  1. Utilidades y helpers (formateo de fechas, duración de pago, etc.).
 *  2. Gestión de clientes (carga, filtros, formularios, acciones activar/desactivar/pausar).
 *  3. Gestión de pagos (listado, filtros, anulación, registro nuevo, multi‑cliente).
 *  4. Reportes (consultas de datos agregados y tablas dinámicas).
 *  5. Dashboard (estadísticas rápidas + actividades recientes).
 *  6. Infraestructura UI (confirmaciones modales, navegación de secciones, delegación de eventos).
 *
 * Principios usados:
 *  - Separar responsabilidades por función (cada función hace 1 cosa clara).
 *  - Evitar duplicar lógica (helpers reutilizados: formatDateForInput, resolveClientIdsFromQuery, etc.).
 *  - Fallbacks tolerantes: si falla un fetch se muestra un mensaje legible.
 *  - No bloquear toda la app por un error parcial: cada carga atrapa excepciones localmente.
 *  - Protección visual: placeholders (“--” o mensajes cortos) mientras no hay datos.
 *
 * Flujo típico (ejemplo para filtros de pagos):
 *  1. Usuario selecciona fechas / estado / cliente.
 *  2. Se construye objeto filters (filterPayments()).
 *  3. loadPayments(filters) arma querystring y hace llamada a /api/payments.
 *  4. Respuesta JSON (Page) -> se extrae page.content -> se ordena -> se pinta tabla.
 *  5. Si error HTTP -> mensaje “No se pudo cargar pagos.” (no rompe otras secciones).
 *
 * Notas de accesibilidad / UX:
 *  - Se usan títulos y textos descriptivos en botones (title="Editar", etc.).
 *  - Botones deshabilitados durante operaciones críticas para evitar doble envío.
 *  - Alertas simples (alert()) para feedback rápido; podría evolucionar a un sistema de toasts.
 *
 * Posibles mejoras futuras (no incluidas pero fáciles de sumar):
 *  - Reemplazar alert/prompts por componentes modales estilizados.
 *  - Paginar tablas grandes (actualmente se trae size=50 por defecto en pagos).
 *  - Spinner / indicador de carga mientras llegan los datos.
 *  - Internacionalización centralizada (hoy strings en español inline).
 *  - Migrar a framework (React/Vue) si crece la complejidad de estado.
 * ============================================================================ */

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

//formatea la duración del pago (solo texto)
function formatPaymentDuration(payment) {
    const days = Number(payment && payment.durationDays);
    if (!Number.isNaN(days) && days > 0) {
        return `${days} día${days === 1 ? '' : 's'}`;
    }
    return '1 mes';
}

//construye el contenido de la celda "Tiempo" con rango
function formatPaymentTimeCell(payment) {
    const durationText = formatPaymentDuration(payment);
    const pd = payment && payment.paymentDate ? new Date(payment.paymentDate) : null;
    const ed = payment && payment.expirationDate ? new Date(payment.expirationDate) : null;
    const fmt = (d) => d ? d.toLocaleDateString() : '-';
    if (pd && ed) {
        return `${durationText} • ${fmt(pd)} → ${fmt(ed)}`;
    }
    return durationText;
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

// ===================== Gestión de Secciones / Navegación =====================
// Navegación simple entre secciones
function showSection(sectionId, skipAuto = false) {
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    const sec = document.getElementById(`${sectionId}-section`);
    if (sec) sec.style.display = 'block';

    if (sectionId === 'dashboard') {
        loadDashboardStats(); // Recargar estadísticas cada vez que se muestra el dashboard
        loadRecentActivities(); // Recargar actividades recientes cada vez que se muestra el dashboard
    }
    if (sectionId === 'clients') loadClients();
    if (sectionId === 'payments') {
        if (!skipAuto) {
            populatePaymentClientFilter().then(() => loadPayments());
        }
    }
    if (sectionId === 'reports') {
        loadReportSummary();
    }
}

function logout() {
    localStorage.removeItem('token');
    window.location.href = '/index.html';
}

// ===================== Clientes =====================
async function loadClients(filters = null) {
    const countEl = document.getElementById('clients-count');
    try {
        const url = new URL('/api/clients', window.location.origin);
        const hasFilters = !!(filters && Object.keys(filters).length);
        if (hasFilters) {
            Object.entries(filters).forEach(([k, v]) => {
                if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v);
            });
        }
        const response = await apiFetch(url.pathname + (url.search || ''));
        if (!response.ok) {
            const tbody = document.querySelector('#clients-table tbody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="7">No se pudo cargar la lista de clientes.</td></tr>';
            if (countEl) countEl.textContent = '--';
            return;
        }
        const clients = await response.json();
        clientsCache = Array.isArray(clients) ? clients : []; // actualizar caché
        const tbody = document.querySelector('#clients-table tbody');
        if (!tbody) {
            if (countEl) countEl.textContent = String(clientsCache.length);
            return;
        }
        if (!Array.isArray(clients) || clients.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7">${hasFilters ? 'Sin resultados para el filtro aplicado.' : 'No hay clientes registrados.'}</td></tr>`;
            if (countEl) countEl.textContent = '0';
            return;
        }
        if (countEl) countEl.textContent = String(clients.length);
        tbody.innerHTML = clients.map(client => {
            const start = client.startDate ? new Date(client.startDate).toLocaleDateString() : '-';
            const pauseBtn = isCurrentlyPaused(client)
                ? `<button type="button" class="action-btn pause js-resume" data-id="${client.id}" title="Reanudar">▶️</button>`
                : `<button type="button" class="action-btn pause js-pause" data-id="${client.id}" title="Pausar">⏸️</button>`;
            return `
            <tr>
                <!-- ID oculto: no se muestra columna de ID -->
                <td class="name-cell">${client.firstName || ''}</td>
                <td class="name-cell">${client.lastName || ''}</td>
                <td class="email-cell">${client.email || ''}</td>
                <td class="phone-cell">${client.phone || '-'}</td>
                <td><span class="status-badge ${client.active ? 'active' : 'inactive'}">${client.active ? 'Activo' : 'Inactivo'}</span></td>
                <td class="date-cell">${start}</td>
                <td class="actions-cell">
                    <button type="button" class="action-btn edit js-edit" data-id="${client.id}" title="Editar">✏️</button>
                    ${pauseBtn}
                    ${client.active ? 
                        `<button type="button" class="action-btn delete js-deactivate" data-id="${client.id}" title="Desactivar">❌</button>` :
                        `<button type="button" class="action-btn edit js-activate" data-id="${client.id}" title="Activar">✅</button>`
                    }
                </td>
            </tr>`;
        }).join('');
    } catch (error) {
        console.error('Error al cargar clientes:', error);
        const tbody = document.querySelector('#clients-table tbody');
        if (tbody) tbody.innerHTML = '<tr><td colspan="7">No se pudo cargar la lista de clientes.</td></tr>';
        if (countEl) countEl.textContent = '--';
    }
}

function getClientFiltersFromDOM() {
    const q = document.getElementById('client-q')?.value?.trim();
    const activeRaw = document.getElementById('client-active')?.value || '';
    const paymentRaw = document.getElementById('client-payment')?.value || '';

    const filters = {};
    if (q) filters.q = q;

    const normalize = (s) => (s || '').trim().toUpperCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');

    // ----- Active -----
    if (activeRaw) {
        const normA = normalize(activeRaw);
        if (['TRUE','ACTIVO','ACTIVOS','ACTIVE'].includes(normA)) {
            filters.active = 'true'; // backend acepta 'true'
        } else if (['FALSE','INACTIVO','INACTIVOS','INACTIVE'].includes(normA)) {
            filters.active = 'false';
        } // 'TODOS' u otros => no se envía
    }

    // ----- Payment -----
    if (paymentRaw) {
        const normP = normalize(paymentRaw).replace(/PAGO:?\s*/,''); // quita prefijo 'PAGO: '
        if (['TODOS','ALL',''].includes(normP)) {
            // no enviar
        } else if (['AL DIA','AL DÍA','ALDIA'].includes(normP)) {
            filters.payment = 'UP_TO_DATE';
        } else if (normP === 'VENCIDO' || normP === 'VENCIDOS') {
            filters.payment = 'EXPIRED';
        } else if (normP === 'ANULADO' || normP === 'ANULADOS') {
            filters.payment = 'VOIDED';
        } else if (['UP_TO_DATE','EXPIRED','VOIDED'].includes(normP)) {
            filters.payment = normP; // ya es técnico
        }
    }
    return filters;
}

function filterClients() {
    const filters = getClientFiltersFromDOM();
    loadClients(filters);
}

function clearClientFilters() {
    const q = document.getElementById('client-q');
    const active = document.getElementById('client-active');
    const payment = document.getElementById('client-payment');
    if (q) q.value = '';
    if (active) active.value = '';
    if (payment) payment.value = '';
    loadClients();
}

// Stubs de acciones de clientes
async function showClientForm() {
    const modal = document.getElementById('client-modal');
    if (!modal) return;
    // Cargar plantilla del formulario
    modal.innerHTML = await fetch('/admin/templates/client-form.html').then(r => r.text());
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
            await loadClients(getClientFiltersFromDOM());
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
                        await loadClients(getClientFiltersFromDOM());
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
// llama a showConfirmation(), que es una función asíncrona que devuelve una promise
//Cuando necesitas esperar el resultado de una operación asíncrona (como la respuesta del usuario en el modal),
// debes usar await, y para poder usar await,
// la función contenedora debe ser async.
async function onClientsTableClick(e) {
    const btn = e.target.closest('button');
    console.debug('[UI] clients table click', { target: e.target, btn });
    if (!btn) return;
    const id = btn.getAttribute('data-id');
    if (btn.classList.contains('js-edit')) {
        console.debug('[UI] edit client', id);
        editClient(Number(id));
        return;
    }
    if (btn.classList.contains('js-view-payments')) {
        viewClientPayments(Number(id));
        return;
    }
    if (btn.classList.contains('js-deactivate')) {
        const client = clientsCache.find(c => String(c.id) === String(id));
        const clientName = client ? `${client.firstName || ''} ${client.lastName || ''}`.trim() : 'este cliente';

        const confirmed = await showConfirmation({
            title: 'Desactivar cliente',
            message: `¿Estás seguro de que quieres desactivar a ${clientName}? Esta acción pausará su membresía y ya no podrá acceder al gimnasio.`,
            icon: 'warning',
            confirmText: 'Sí, desactivar',
            cancelText: 'Cancelar',
            type: 'danger'
        });

        if (!confirmed) return;

        const prev = btn.textContent; btn.disabled = true; btn.textContent = 'Desactivando…';
        apiFetch(`/api/clients/${id}`, { method: 'DELETE', noRedirectOn401: true })
          .then(res => {
            if (res.status === 401) { alert('Sesión expirada. Inicia sesión nuevamente para continuar.'); return; }
            if (!res.ok) return res.text().then(t => alert(t || 'No se pudo desactivar el cliente'));
            alert('Cliente desactivado exitosamente');
            loadClients(getClientFiltersFromDOM());
          })
          .catch(() => alert('Error de red'))
          .finally(() => { btn.disabled = false; btn.textContent = prev; });
        return;
    }
    if (btn.classList.contains('js-activate')) {
        const client = clientsCache.find(c => String(c.id) === String(id));
        const clientName = client ? `${client.firstName || ''} ${client.lastName || ''}`.trim() : 'este cliente';

        const confirmed = await showConfirmation({
            title: 'Activar cliente',
            message: `¿Deseas reactivar la membresía de ${clientName}? Podrá volver a acceder al gimnasio.`,
            icon: 'info',
            confirmText: 'Sí, activar',
            cancelText: 'Cancelar',
            type: 'primary'
        });

        if (!confirmed) return;

        const prev = btn.textContent; btn.disabled = true; btn.textContent = 'Activando…';
        apiFetch(`/api/clients/${id}/activate`, { method: 'POST', noRedirectOn401: true })
          .then(res => {
            if (res.status === 401) { alert('Sesión expirada. Inicia sesión nuevamente para continuar.'); return; }
            if (!res.ok) return res.text().then(t => alert(t || 'No se pudo activar el cliente'));
            alert('Cliente activado exitosamente');
            loadClients(getClientFiltersFromDOM());
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
        const client = clientsCache.find(c => String(c.id) === String(id));
        const clientName = client ? `${client.firstName || ''} ${client.lastName || ''}`.trim() : 'este cliente';

        const confirmed = await showConfirmation({
            title: 'Reanudar suscripción',
            message: `¿Quieres reanudar la suscripción de ${clientName}? La pausa se cancelará y volverá a estar activo.`,
            icon: 'info',
            confirmText: 'Sí, reanudar',
            cancelText: 'Cancelar',
            type: 'primary'
        });

        if (!confirmed) return;

        const prev = btn.textContent; btn.disabled = true; btn.textContent = 'Reanudando…';
        apiFetch(`/api/clients/${id}/resume`, { method: 'POST', noRedirectOn401: true })
          .then(res => {
            if (res.status === 401) { alert('Sesión expirada. Inicia sesión nuevamente para continuar.'); return; }
            if (!res.ok) return res.text().then(t => alert(t || 'No se pudo reanudar la suscripción'));
            alert('Suscripción reanudada exitosamente');
            loadClients(getClientFiltersFromDOM());
          })
          .catch(() => alert('Error de red'))
          .finally(() => { btn.disabled = false; btn.textContent = prev; });
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
        const fromInput = modal.querySelector('#pauseFrom');
        const toInput = modal.querySelector('#pauseTo');
        const durationEl = modal.querySelector('#pause-duration');
        modal.querySelector('#pause-client-id').value = clientId;

        // Helpers locales: parseo seguro YYYY-MM-DD y cálculo inclusivo en días
        const parseYmdToUtc = (ymd) => {
            if (!ymd || !/^\d{4}-\d{2}-\d{2}$/.test(ymd)) return null;
            const [y, m, d] = ymd.split('-').map(Number);
            return new Date(Date.UTC(y, m - 1, d));
        };
        const formatDaysText = (days) => `${days} día${days === 1 ? '' : 's'}`;
        const msPerDay = 24 * 60 * 60 * 1000;

        function updateToMin() {
            if (fromInput && toInput && fromInput.value) {
                toInput.min = fromInput.value;
            }
        }

        function updateSummaryAndValidation() {
            if (errEl) errEl.textContent = '';
            if (!fromInput || !toInput || !durationEl) return;
            const from = parseYmdToUtc(fromInput.value);
            const to = parseYmdToUtc(toInput.value);

            // Validar presencia de fechas
            if (!from || !to) {
                durationEl.textContent = '-';
                return;
            }

            // Validar rango correcto
            if (to < from) {
                durationEl.textContent = '-';
                if (errEl) errEl.textContent = 'La fecha "Hasta" no puede ser anterior a "Desde"';
                return;
            }

            // Calcular duración inclusiva en días evitando problemas de huso/DST
            const diffDays = Math.floor((to.getTime() - from.getTime()) / msPerDay) + 1;
            durationEl.textContent = formatDaysText(diffDays);
        }

        function prefillDefaults() {
            if (!fromInput || !toInput) return;
            const today = new Date();
            const todayYmd = formatDateForInput(today);
            // Por defecto: 1 semana (7 días inclusivos) => to = today + 6
            const weekLater = new Date(today);
            weekLater.setDate(weekLater.getDate() + 6);
            const weekLaterYmd = formatDateForInput(weekLater);

            // Limitar fechas mínimas para evitar pasado
            fromInput.min = todayYmd;
            if (!fromInput.value) fromInput.value = todayYmd;
            updateToMin();
            if (!toInput.value) toInput.value = weekLaterYmd;
            updateSummaryAndValidation();
        }

        // Listeners de cambio para feedback inmediato
        if (fromInput) {
            fromInput.addEventListener('input', () => {
                updateToMin();
                // Si "hasta" quedó por debajo del nuevo "desde", ajústalo al mismo día
                if (toInput && toInput.value && toInput.value < fromInput.value) {
                    toInput.value = fromInput.value;
                }
                updateSummaryAndValidation();
            });
            fromInput.addEventListener('change', () => {
                updateToMin();
                updateSummaryAndValidation();
            });
        }
        if (toInput) {
            toInput.addEventListener('input', updateSummaryAndValidation);
            toInput.addEventListener('change', updateSummaryAndValidation);
        }

        // Prefill y primer cálculo
        prefillDefaults();

        // Envío del formulario
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (errEl) errEl.textContent = '';
            const from = fromInput?.value;
            const to = toInput?.value;
            const reason = form.querySelector('#pauseReason').value.trim();

            if (!from || !to) {
                if (errEl) errEl.textContent = 'Debe completar fechas desde y hasta';
                return;
            }
            if (new Date(to) < new Date(from)) {
                if (errEl) errEl.textContent = 'La fecha "Hasta" no puede ser anterior a "Desde"';
                return;
            }

            const btn = form.querySelector('button[type="submit"]');
            const prev = btn.textContent; btn.disabled = true; btn.textContent = 'Pausando…';
            try {
                const qs = new URLSearchParams({ from, to });
                if (reason) qs.append('reason', reason);
                const res = await apiFetch(`/api/clients/${clientId}/pause?${qs.toString()}`, { method: 'POST', noRedirectOn401: true });
                if (res.status === 401) { if (errEl) errEl.textContent = 'Sesión expirada. Inicia sesión nuevamente para continuar.'; return; }
                if (!res.ok) { const msg = await res.text(); if (errEl) errEl.textContent = msg || 'No se pudo pausar la suscripción'; return; }
                closeModal('pause-modal');
                loadClients(getClientFiltersFromDOM());
                alert('Suscripción pausada');
            } catch {
                if (errEl) errEl.textContent = 'Error de red';
            } finally {
                btn.disabled = false; btn.textContent = prev;
            }
        });
      });
}

// ===================== Pagos =====================
// loadPayments: núcleo de la vista de pagos.
// Dos modos:
//  1) Múltiples clientIds: se hacen varias peticiones en paralelo (Promise.allSettled) y se combinan resultados.
//  2) Modo estándar: una petición paginada (Page) con filtros.
// Reglas de ordenamiento secundarias: primero por paymentDate desc, luego por id desc para consistencia.
// Protección ante respuestas no esperadas: intenta detectar si es Page (content) o lista directa.
async function loadPayments(filters = {}) {
    const countEl = document.getElementById('payments-count');
    try {
        // Si viene un arreglo de clientIds, hacemos múltiples requests y mergeamos
        if (Array.isArray(filters.clientIds) && filters.clientIds.length > 1) {
            const { clientIds, from, to, state } = filters;
            const queries = clientIds.map(id => {
                const params = new URLSearchParams();
                params.set('clientId', String(id));
                if (from) params.set('from', from);
                if (to) params.set('to', to);
                if (state) params.set('state', state);
                params.set('size', '50');
                return apiFetch('/api/payments?' + params.toString());
            });
            const responses = await Promise.allSettled(queries);
            const items = [];
            for (const r of responses) {
                if (r.status === 'fulfilled' && r.value.ok) {
                    const data = await r.value.json();
                    const list = Array.isArray(data) ? data : (Array.isArray(data.content) ? data.content : []);
                    items.push(...list);
                }
            }
            const tbody = document.querySelector('#payments-table tbody');
            if (!tbody) {
                if (countEl) countEl.textContent = String(items.length || 0);
                return;
            }
            if (items.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6">Sin resultados.</td></tr>';
                if (countEl) countEl.textContent = '0';
                return;
            }
            const sortedItems = items.slice().sort((a, b) => {
                const ta = a && a.paymentDate ? new Date(a.paymentDate).getTime() : 0;
                const tb = b && b.paymentDate ? new Date(b.paymentDate).getTime() : 0;
                if (tb !== ta) return tb - ta;
                const ida = Number(a && a.id) || 0;
                const idb = Number(b && b.id) || 0;
                return idb - ida;
            });
            if (countEl) countEl.textContent = String(items.length);
            tbody.innerHTML = sortedItems.map(payment => {
                const amount = Number(payment.amount || 0).toFixed(2);
                const dateStr = payment.paymentDate ? new Date(payment.paymentDate).toLocaleDateString() : '-';
                const timeCell = formatPaymentTimeCell(payment);
                // Preferir datos del backend en PaymentDTO
                const fromDtoName = `${payment.clientFirstName || ''} ${payment.clientLastName || ''}`.trim();
                const fromDtoEmail = payment.clientEmail || '';
                let clientText = fromDtoName || fromDtoEmail || '';
                if (!clientText) {
                    const client = Array.isArray(clientsCache) ? clientsCache.find(c => String(c.id) === String(payment.clientId)) : null;
                    clientText = client ? (`${client.firstName || ''} ${client.lastName || ''}`.trim() || (client.email || '—')) : '—';
                }
                const actions = !payment.voided
                    ? `<button class="payments-table-action-btn" onclick="voidPayment(${payment.id})">Anular</button>`
                    : '<span class="status-badge voided">Anulado</span>';
                return `
                <tr>
                    <td class="name-cell">${clientText || '—'}</td>
                    <td style="text-align:right">$${amount}</td>
                    <td>${payment.method || '-'}</td>
                    <td class="date-cell">${dateStr}</td>
                    <td class="duration-cell">${timeCell}</td>
                    <td class="actions-cell">${actions}</td>
                </tr>`;
            }).join('');
            return;
        }
        // flujo original (un solo cliente o sin clientId)
        const params = new URLSearchParams(filters);
        if (!params.has('size')) params.set('size', '50');
        const response = await apiFetch('/api/payments?' + params.toString());
        if (!response.ok) {
            const tbody = document.querySelector('#payments-table tbody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="6">No se pudo cargar pagos.</td></tr>';
            if (countEl) countEl.textContent = '--';
            return;
        }
        const pageOrList = await response.json();
        const tbody = document.querySelector('#payments-table tbody');
        if (!tbody) {
            const totalCount = Array.isArray(pageOrList)
                ? pageOrList.length
                : (Number.isFinite(pageOrList?.totalElements) ? pageOrList.totalElements : (Array.isArray(pageOrList?.content) ? pageOrList.content.length : 0));
            if (countEl) countEl.textContent = String(totalCount);
            return;
        }
        const items = Array.isArray(pageOrList)
            ? pageOrList
            : Array.isArray(pageOrList.content) ? pageOrList.content : [];
        const totalCount = Array.isArray(pageOrList)
            ? pageOrList.length
            : (Number.isFinite(pageOrList?.totalElements) ? pageOrList.totalElements : items.length);
        if (items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6">Sin resultados.</td></tr>';
            if (countEl) countEl.textContent = String(totalCount || 0);
            return;
        }
        // Ordenar: primero por fecha de pago descendente, y si falta/empata, por id descendente
        const sortedItems = items.slice().sort((a, b) => {
            const ta = a && a.paymentDate ? new Date(a.paymentDate).getTime() : 0;
            const tb = b && b.paymentDate ? new Date(b.paymentDate).getTime() : 0;
            if (tb !== ta) return tb - ta; // más reciente primero
            const ida = Number(a && a.id) || 0;
            const idb = Number(b && b.id) || 0;
            return idb - ida; // id más alto primero
        });
        if (countEl) countEl.textContent = String(totalCount);
        tbody.innerHTML = sortedItems.map(payment => {
            const amount = Number(payment.amount || 0).toFixed(2);
            const dateStr = payment.paymentDate ? new Date(payment.paymentDate).toLocaleDateString() : '-';
            const timeCell = formatPaymentTimeCell(payment);
            // Preferir datos del backend en PaymentDTO
            const fromDtoName = `${payment.clientFirstName || ''} ${payment.clientLastName || ''}`.trim();
            const fromDtoEmail = payment.clientEmail || '';
            let clientText = fromDtoName || fromDtoEmail || '';
            if (!clientText) {
                const client = Array.isArray(clientsCache) ? clientsCache.find(c => String(c.id) === String(payment.clientId)) : null;
                clientText = client ? (`${client.firstName || ''} ${client.lastName || ''}`.trim() || (client.email || '—')) : '—';
            }
            const actions = !payment.voided
                ? `<button class=\"payments-table-action-btn\" onclick=\"voidPayment(${payment.id})\">Anular</button>`
                : '<span class="status-badge voided">Anulado</span>';
            return `
            <tr>
                <td class="name-cell">${clientText || '—'}</td>
                <td style="text-align:right">$${amount}</td>
                <td>${payment.method || '-'}</td>
                <td class="date-cell">${dateStr}</td>
                <td class="duration-cell">${timeCell}</td>
                <td class="actions-cell">${actions}</td>
            </tr>`;
        }).join('');
    } catch (error) {
        console.error('Error al cargar pagos:', error);
        const countEl2 = document.getElementById('payments-count');
        if (countEl2) countEl2.textContent = '--';
    }
}

// populatePaymentClientFilter: llena un datalist HTML para autocompletar clientes en filtros de pagos.
// Carga clientes solo si no están cacheados (patrón: lazy loading en memoria).
async function populatePaymentClientFilter() {
    const datalist = document.getElementById('payment-client-options');
    const input = document.getElementById('payment-client-q');
    if (!datalist) return;
    const prev = input ? input.value : '';
    try {
        if (!Array.isArray(clientsCache) || clientsCache.length === 0) {
            const res = await apiFetch('/api/clients');
            if (res.ok) {
                const list = await res.json();
                clientsCache = Array.isArray(list) ? list : [];
            }
        }
        datalist.innerHTML = (clientsCache || []).map(c => {
            const name = `${c.firstName || ''} ${c.lastName || ''}`.trim();
            const email = c.email || '';
            return `<option value="${c.id} - ${name} (${email})"></option>`;
        }).join('');
        if (input && prev) input.value = prev;
    } catch (_) {
        if (datalist) datalist.innerHTML = '';
    }
}

// filterPayments: construye objeto filters a partir de inputs, resolviendo nombres/textos a IDs.
// Edge case: si el usuario escribe algo que no coincide con ningún cliente se evita llamar al backend y se muestra tabla vacía.
function filterPayments() {
    const from = document.getElementById('date-from')?.value;
    const to = document.getElementById('date-to')?.value;
    const state = document.getElementById('payment-state')?.value;
    const q = document.getElementById('payment-client-q')?.value?.trim();
    const ids = resolveClientIdsFromQuery(q);
    // Si hay texto y no hay coincidencias, mostrar tabla vacía
    if (q && ids.length === 0) {
        const tbody = document.querySelector('#payments-table tbody');
        if (tbody) tbody.innerHTML = '<tr><td colspan="6">Sin resultados.</td></tr>';
        const countEl = document.getElementById('payments-count');
        if (countEl) countEl.textContent = '0';
        return;
    }
    const filters = {};
    if (ids.length > 1) {
        filters.clientIds = ids;
    } else if (ids.length === 1) {
        filters.clientId = ids[0];
    }
    if (from) filters.from = from;
    if (to) filters.to = to;
    if (state) filters.state = state;
    loadPayments(filters);
}

// Añadido: limpiar filtros de pagos
function clearPaymentFilters() {
    const input = document.getElementById('payment-client-q');
    const from = document.getElementById('date-from');
    const to = document.getElementById('date-to');
    const state = document.getElementById('payment-state');
    if (input) input.value = '';
    if (from) from.value = '';
    if (to) to.value = '';
    if (state) state.value = '';
    loadPayments();
}

// Añadido: ver pagos de un cliente (navega y filtra)
function viewClientPayments(clientId) {
    // Evita carga automática para que no se pisen filtros
    showSection('payments', true);
    const input = document.getElementById('payment-client-q');
    // Garantiza que el datalist esté poblado (si aún no lo está)
    populatePaymentClientFilter().then(() => {
        if (input) {
            const c = (clientsCache || []).find(x => Number(x.id) === Number(clientId));
            const name = c ? `${c.firstName || ''} ${c.lastName || ''}`.trim() : '';
            const email = c?.email || '';
            input.value = c ? `${c.id} - ${name} (${email})` : String(clientId);
        }
    });
    // Cargar pagos del cliente
    loadPayments({ clientId });
}

function voidPayment(id) {
    (async () => {
        const confirmed = await showConfirmation({
            title: 'Anular pago',
            message: '¿Deseas anular este pago? Esta acción no puede deshacerse.',
            icon: 'warning',
            confirmText: 'Anular',
            cancelText: 'Cancelar',
            type: 'danger'
        });
        if (!confirmed) return;
        const reason = prompt('Motivo de anulación (opcional):', 'Anulado desde panel') || 'Anulado desde panel';
        try {
            const res = await apiFetch(`/api/payments/${id}/void?reason=${encodeURIComponent(reason)}`, { method: 'POST', noRedirectOn401: true });
            const text = await res.text();
            if (!res.ok) {
                alert(text || 'No se pudo anular el pago');
                return;
            }
            alert('Pago anulado');
            // Recargar con filtros actuales
            const from = document.getElementById('date-from')?.value;
            const to = document.getElementById('date-to')?.value;
            const state = document.getElementById('payment-state')?.value;
            const q = document.getElementById('payment-client-q')?.value?.trim();
            const ids = resolveClientIdsFromQuery(q);
            const filters = {};
            if (ids.length > 1) filters.clientIds = ids;
            else if (ids.length === 1) filters.clientId = ids[0];
            if (from) filters.from = from;
            if (to) filters.to = to;
            if (state) filters.state = state;
            await loadPayments(filters);
        } catch (_) {
            alert('Error de red');
        }
    })();
}

async function showPaymentForm() {
    const modal = document.getElementById('payment-modal');
    if (!modal) return;
    modal.innerHTML = await fetch('/admin/templates/payment-form.html').then(r => r.text());
    modal.style.display = 'flex';
    document.body.style.overflow = 'hidden';
    modal.onclick = (e) => { if (e.target === modal) closeModal('payment-modal'); };

    const form = modal.querySelector('#payment-form');
    const errEl = modal.querySelector('#payment-form-error');
    // nuevos elementos para búsqueda rápida de cliente
    const clientSearch = form.querySelector('#clientSearch');
    const clientIdHidden = form.querySelector('#clientId');
    const clientOptions = form.querySelector('#clientOptions');
    const periodMonth = form.querySelector('#periodMonth');
    const periodYear = form.querySelector('#periodYear');
    const validityRadios = form.querySelectorAll('input[name="validityType"]');
    const durationGroup = form.querySelector('#durationDaysGroup');
    const durationInput = form.querySelector('#durationDays');

    // Poblar clientes activos en el datalist
    try {
        if (!Array.isArray(clientsCache) || clientsCache.length === 0) {
            const res = await apiFetch('/api/clients');
            const list = await res.json();
            clientsCache = Array.isArray(list) ? list : [];
        }
        const actives = clientsCache.filter(c => c.active);
        if (clientOptions) {
            clientOptions.innerHTML = actives.map(c => {
                const name = `${c.firstName || ''} ${c.lastName || ''}`.trim();
                const email = c.email || '';
                return `<option value="${c.id} - ${name} (${email})"></option>`;
            }).join('');
        }
    } catch { /* ignore */ }

    // Sincronizar input con hidden: permite pegar ID o elegir de lista
    function syncClientIdFromSearch() {
        const v = (clientSearch?.value || '').trim();
        if (!v) { clientIdHidden.value = ''; return; }
        // Si comienza con números, tomar ese ID
        const m = v.match(/^\d+/);
        if (m) {
            const id = Number(m[0]);
            const exists = Array.isArray(clientsCache) && clientsCache.some(c => Number(c.id) === id);
            clientIdHidden.value = exists ? String(id) : '';
            return;
        }
        // Buscar por coincidencia de texto (nombre/email)
        const found = (clientsCache || []).find(c => {
            const name = `${c.firstName || ''} ${c.lastName || ''}`.trim().toLowerCase();
            const email = (c.email || '').toLowerCase();
            return name.includes(v.toLowerCase()) || email.includes(v.toLowerCase());
        });
        clientIdHidden.value = found ? String(found.id) : '';
    }
    if (clientSearch) {
        clientSearch.addEventListener('input', syncClientIdFromSearch);
        clientSearch.addEventListener('change', syncClientIdFromSearch);
        clientSearch.addEventListener('blur', syncClientIdFromSearch);
    }

    // Defaults de período
    const today = new Date();
    periodMonth.value = String(today.getMonth() + 1);
    periodYear.value = String(today.getFullYear());

    // Toggle duración por días
    validityRadios.forEach(r => r.addEventListener('change', () => {
        const byDays = form.querySelector('input[name="validityType"]:checked')?.value === 'days';
        durationGroup.style.display = byDays ? 'block' : 'none';
        if (!byDays) { durationInput.value = ''; }
    }));

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (errEl) errEl.textContent = '';
        // tomar id oculto mapeado por la búsqueda
        const clientId = Number(clientIdHidden.value);
        if (!clientId || Number.isNaN(clientId)) {
            if (errEl) errEl.textContent = 'Selecciona un cliente válido (escribe y elige una opción, o pega el ID).';
            clientSearch?.focus();
            return;
        }
        const amount = Number(form.querySelector('#amount').value);
        const method = form.querySelector('#method').value;
        const paymentDate = form.querySelector('#paymentDate').value || null;
        const month = Number(periodMonth.value);
        const year = Number(periodYear.value);
        const byDays = form.querySelector('input[name="validityType"]:checked')?.value === 'days';
        const durationDays = byDays ? Number(durationInput.value) : null;

        const payload = { clientId, amount, method, month, year };
        if (paymentDate) payload.paymentDate = paymentDate;
        if (byDays && durationDays) payload.durationDays = durationDays;

        try {
            const res = await apiFetch('/api/payments', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
                noRedirectOn401: true
            });
            if (res.status === 401) { if (errEl) errEl.textContent = 'Sesión expirada. Inicia sesión nuevamente.'; return; }
            const text = await res.text();
            if (!res.ok) {
                const duplicate = res.status === 409 || /existe un pago válido/i.test(text);
                if (errEl) errEl.textContent = duplicate ? 'Ya existe un pago válido para ese período.' : (text || 'No se pudo registrar el pago');
                return;
            }
            closeModal('payment-modal');
            // Recargar respetando filtros actuales para que el nuevo pago aparezca primero con el orden aplicado
            const from = document.getElementById('date-from')?.value;
            const to = document.getElementById('date-to')?.value;
            const state = document.getElementById('payment-state')?.value;
            const q = document.getElementById('payment-client-q')?.value?.trim();
            const ids = resolveClientIdsFromQuery(q);
            const filters = {};
            if (ids.length > 1) filters.clientIds = ids;
            else if (ids.length === 1) filters.clientId = ids[0];
            if (from) filters.from = from;
            if (to) filters.to = to;
            if (state) filters.state = state;
            await loadPayments(filters);
            alert('Pago registrado');
        } catch {
            if (errEl) errEl.textContent = 'Error de red';
        }
    });
}

// ===================== Reportes =====================
// Cada reporte hace fetch aislado y reutiliza showReportResults para pintar contenido.
// showReportResults detecta si recibe texto simple (flujo de caja) o lista de clientes/pagos.
// hasExpiration decide si renderiza columna “Fecha de expiración”.
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

async function loadReportSummary() {
    // elementos destino
    const expiredEl = document.getElementById('report-expired-count');
    const expiringEl = document.getElementById('report-expiring-count');
    const incomeEl = document.getElementById('report-month-income');
    if (expiredEl) expiredEl.textContent = '--';
    if (expiringEl) expiringEl.textContent = '--';
    if (incomeEl) incomeEl.textContent = '--';

    // helpers de fechas (mes actual)
    const now = new Date();
    const first = new Date(now.getFullYear(), now.getMonth(), 1);
    const last = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    const ymd = (d) => formatDateForInput(d);

    try {
        // vencidos (conteo)
        const overdueRes = await apiFetch('/api/reports/overdue');
        if (overdueRes && overdueRes.ok && expiredEl) {
            const list = await overdueRes.json();
            expiredEl.textContent = Array.isArray(list) ? String(list.length) : '0';
        }
    } catch (_) { /* noop */ }

    try {
        // por vencer (7 días) - ya existe endpoint
        const expiringRes = await apiFetch('/api/reports/expiring');
        if (expiringRes && expiringRes.ok && expiringEl) {
            const list = await expiringRes.json();
            expiringEl.textContent = Array.isArray(list) ? String(list.length) : '0';
        }
    } catch (_) { /* noop */ }

    try {
        // ingresos del mes (flujo de caja)
        const cfRes = await apiFetch(`/api/reports/cashflow?from=${ymd(first)}&to=${ymd(last)}`);
        if (cfRes && cfRes.ok && incomeEl) {
            const total = await cfRes.json();
            const num = Number(total || 0);
            incomeEl.textContent = `$${num.toFixed(2)}`;
        }
    } catch (_) { /* noop */ }
}

function showReportResults(title, data) {
    const resultsDiv = document.getElementById('report-results');
    if (!resultsDiv) return;

    // Si es texto (ej: Flujo de caja), mostrar bloque simple en estilo oscuro
    if (typeof data === 'string') {
        resultsDiv.innerHTML = `
            <div class="clients-table-container">
                <div class="table-header"><h3>${title}</h3></div>
                <div class="modern-table-wrapper">
                    <div style="padding:1rem;color:#e5e7eb;">${data}</div>
                </div>
            </div>`;
        return;
    }

    // Normalizar data a arreglo
    const rows = Array.isArray(data) ? data : [];
    const empty = rows.length === 0;

    // helper: formatea YYYY-MM-DD a dd/mm/yyyy sin problemas de timezone
    const fmtYmd = (s) => {
        if (!s || typeof s !== 'string') return '';
        const m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/);
        if (!m) { try { return new Date(s).toLocaleDateString(); } catch { return s; } }
        return `${Number(m[3])}/${Number(m[2])}/${m[1]}`;
    };

    // ¿Este reporte trae fecha de expiración?
    const hasExpiration = !empty && Object.prototype.hasOwnProperty.call(rows[0], 'expirationDate');

    const tableHtml = empty
        ? `<div class="clients-table-container"><div class="table-header"><h3>${title}</h3></div><div class="modern-table-wrapper"><div style="padding:1rem;color:#8b9dc3;">Sin clientes para mostrar.</div></div></div>`
        : `<div class="clients-table-container">
             <div class="table-header"><h3>${title}</h3></div>
             <div class="modern-table-wrapper">
               <table class="modern-table">
                 <thead>
                   <tr>
                     <th>Cliente</th>
                     <th>Email</th>
                     ${hasExpiration ? '<th>Fecha de expiración</th>' : '<th>Estado</th>'}
                   </tr>
                 </thead>
                 <tbody>
                   ${rows.map(client => {
                        const name = `${(client.firstName||'')} ${(client.lastName||'')}`.trim();
                        const email = client.email || '';
                        if (hasExpiration) {
                            const exp = fmtYmd(client.expirationDate);
                            return `<tr>
                                      <td class="name-cell">${name}</td>
                                      <td class="email-cell">${email}</td>
                                      <td class="date-cell">${exp}</td>
                                    </tr>`;
                        }
                        const active = client.active ? 'Activo' : 'Inactivo';
                        const badgeCls = client.active ? 'active' : 'inactive';
                        return `<tr>
                                  <td class="name-cell">${name}</td>
                                  <td class="email-cell">${email}</td>
                                  <td><span class="status-badge ${badgeCls}">${active}</span></td>
                                </tr>`;
                   }).join('')}
                 </tbody>
               </table>
             </div>
           </div>`;

    resultsDiv.innerHTML = tableHtml;
}

// ===================== Modal de Confirmación =====================
// showConfirmation: implementa un modal genérico con Promise para un flujo async limpio (await en llamadas).
// Patrón: la función construye handlers, muestra modal, y resuelve true/false según acción del usuario.
// hideConfirmation: encapsula la lógica de cierre y limpieza de listeners.
function showConfirmation({ title, message, icon = 'warning', confirmText = 'Confirmar', cancelText = 'Cancelar', type = 'danger' }) {
    return new Promise((resolve) => {
        const modal = document.getElementById('confirmation-modal');
        const titleEl = document.getElementById('confirmation-title');
        const messageEl = document.getElementById('confirmation-message');
        const iconEl = document.getElementById('confirmation-icon');
        const confirmBtn = document.getElementById('confirmation-confirm');
        const cancelBtn = document.getElementById('confirmation-cancel');
        const dialog = modal.querySelector('.confirmation-content');

        // Guardar el elemento con foco para restaurarlo al cerrar
        const previouslyFocused = document.activeElement;

        // Configurar contenido
        titleEl.textContent = title;
        messageEl.textContent = message;
        confirmBtn.textContent = confirmText;
        cancelBtn.textContent = cancelText;

        // Configurar icono y tipo
        iconEl.className = `confirmation-icon ${icon}`;
        confirmBtn.className = type === 'danger' ? 'btn-confirm' : 'btn-confirm primary';

        if (icon === 'warning') {
            iconEl.textContent = '⚠️';
        } else if (icon === 'info') {
            iconEl.textContent = 'ℹ️';
        }

        // Mostrar modal
        modal.classList.add('show');
        document.body.style.overflow = 'hidden';

        // Establecer foco inicial amigable
        // Para acciones peligrosas, enfocamos "Cancelar" por defecto.
        // Para acciones informativas/primarias, enfocamos "Confirmar".
        const initialFocusEl = (type === 'danger') ? cancelBtn : confirmBtn;
        // Mover foco al diálogo primero para lectores de pantalla, luego al botón inicial
        if (dialog) dialog.focus();
        setTimeout(() => initialFocusEl?.focus(), 0);

        // Focus trap dentro del modal (Tab y Shift+Tab ciclan)
        const focusableSelectors = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';
        const getFocusable = () => Array.from(dialog.querySelectorAll(focusableSelectors))
            .filter(el => !el.hasAttribute('disabled') && el.offsetParent !== null);

        const handleKeydown = (e) => {
            if (e.key === 'Escape') {
                handleCancel();
                return;
            }
            if (e.key === 'Enter') {
                // Evitar submit implícito inesperado si hay formularios embebidos (no es el caso aquí)
                e.preventDefault();
                handleConfirm();
                return;
            }
            if (e.key === 'Tab') {
                const focusable = getFocusable();
                if (focusable.length === 0) return;
                const first = focusable[0];
                const last = focusable[focusable.length - 1];
                if (e.shiftKey) {
                    if (document.activeElement === first || document.activeElement === dialog) {
                        e.preventDefault();
                        last.focus();
                    }
                } else {
                    if (document.activeElement === last) {
                        e.preventDefault();
                        first.focus();
                    }
                }
            }
        };

        const handleConfirm = () => {
            hideConfirmation();
            if (previouslyFocused && previouslyFocused.focus) {
                setTimeout(() => previouslyFocused.focus(), 0);
            }
            resolve(true);
        };

        const handleCancel = () => {
            hideConfirmation();
            if (previouslyFocused && previouslyFocused.focus) {
                setTimeout(() => previouslyFocused.focus(), 0);
            }
            resolve(false);
        };

        // Limpiar listeners anteriores
        confirmBtn.replaceWith(confirmBtn.cloneNode(true));
        cancelBtn.replaceWith(cancelBtn.cloneNode(true));

        // Añadir nuevos listeners
        const newConfirmBtn = document.getElementById('confirmation-confirm');
        const newCancelBtn = document.getElementById('confirmation-cancel');

        newConfirmBtn.addEventListener('click', handleConfirm);
        newCancelBtn.addEventListener('click', handleCancel);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) handleCancel();
        });
        // Escuchar eventos de teclado en el documento para capturar Escape y Tab
        document.addEventListener('keydown', handleKeydown, true);

        // Limpiar al cerrar
        modal._cleanup = () => {
            document.removeEventListener('keydown', handleKeydown, true);
        };
    });
}

function hideConfirmation() {
    const modal = document.getElementById('confirmation-modal');
    modal.classList.remove('show');
    document.body.style.overflow = '';

    // Ejecutar cleanup si existe
    if (modal._cleanup) {
        modal._cleanup();
        modal._cleanup = null;
    }
}

// ===================== Dashboard =====================
// loadDashboardStats / loadRecentActivities: llamadas independientes para que un fallo no afecte la otra.
// Diseño tolerante: placeholders '--' o mensajes de error contextual.
async function loadDashboardStats() {
    try {
        const response = await apiFetch('/api/dashboard/stats');
        const activeEl = document.getElementById('active-clients-count');
        const pendingEl = document.getElementById('pending-payments-count');
        if (response && response.ok) {
            const { activeClients, expiredPayments } = await response.json();
            if (activeEl) activeEl.textContent = String(activeClients ?? '--');
            if (pendingEl) pendingEl.textContent = String(expiredPayments ?? '--');
        } else {
            if (activeEl) activeEl.textContent = '--';
            if (pendingEl) pendingEl.textContent = '--';
        }
    } catch (_) {
        const activeEl = document.getElementById('active-clients-count');
        const pendingEl = document.getElementById('pending-payments-count');
        if (activeEl) activeEl.textContent = '--';
        if (pendingEl) pendingEl.textContent = '--';
    }
}

async function loadRecentActivities() {
    try {
        const response = await apiFetch('/api/dashboard/activities?limit=6');
        const list = document.getElementById('recent-activity-list');
        if (!list) return;
        if (response && response.ok) {
            const activities = await response.json();
            if (!activities || activities.length === 0) {
                list.innerHTML = `
                    <div class="activity-item">
                        <div class="activity-indicator new-client"></div>
                        <div class="activity-content">
                            <span class="activity-title">No hay actividades recientes</span>
                            <span class="activity-time">Últimos 7 días</span>
                        </div>
                    </div>
                `;
                return;
            }
            list.innerHTML = activities.map(a => {
                const cls = getActivityClass(a.type);
                const timeAgo = formatTimeAgo(a.timestamp);
                return `
                    <div class="activity-item">
                        <div class="activity-indicator ${cls}"></div>
                        <div class="activity-content">
                            <span class="activity-title">${a.title}</span>
                            <span class="activity-time">${timeAgo}</span>
                        </div>
                    </div>
                `;
            }).join('');
        } else {
            list.innerHTML = `
                <div class="activity-item">
                    <div class="activity-indicator new-client"></div>
                    <div class="activity-content">
                        <span class="activity-title">Error al cargar actividades</span>
                        <span class="activity-time">Intenta recargar la página</span>
                    </div>
                </div>
            `;
        }
    } catch (_) {
        const list = document.getElementById('recent-activity-list');
        if (list) {
            list.innerHTML = `
                <div class="activity-item">
                    <div class="activity-indicator new-client"></div>
                    <div class="activity-content">
                        <span class="activity-title">Error al cargar actividades</span>
                        <span class="activity-time">Intenta recargar la página</span>
                    </div>
                </div>
            `;
        }
    }
}

function getActivityClass(type) {
    switch (type) {
        case 'new-client': return 'new-client';
        case 'payment': return 'payment';
        case 'expiring': return 'expiring';
        default: return 'new-client';
    }
}

function formatTimeAgo(timestamp) {
    const now = new Date();
    const activityTime = new Date(timestamp);
    const diffInHours = Math.floor((now - activityTime) / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInHours / 24);

    if (diffInHours < 1) {
        return 'Hace menos de 1 hora';
    } else if (diffInHours < 24) {
        return `Hace ${diffInHours} hora${diffInHours > 1 ? 's' : ''}`;
    } else if (diffInDays === 1) {
        return 'Hace 1 día';
    } else if (diffInDays < 7) {
        return `Hace ${diffInDays} días`;
    } else {
        return activityTime.toLocaleDateString('es-ES', {
            day: 'numeric',
            month: 'short'
        });
    }
}

// ============= Inicio =============
document.addEventListener('DOMContentLoaded', () => {
    // Si no hay token, la API devolverá 401 y apiFetch redirigirá a /index.html
    loadClients();
    loadDashboardStats(); // Cargar estadísticas del dashboard
    loadRecentActivities(); // Cargar actividades recientes del dashboard

    const stateSel = document.getElementById('payment-state');
    if (stateSel) stateSel.addEventListener('change', filterPayments);
    const clientsTable = document.getElementById('clients-table');
    if (clientsTable) clientsTable.addEventListener('click', onClientsTableClick);

    const q = document.getElementById('client-q');
    if (q) q.addEventListener('keyup', (e) => { if (e.key === 'Enter') filterClients(); });

    // prellenar selector de clientes de filtros de pagos
    populatePaymentClientFilter();

    // Binding explícito del botón de limpiar pagos (fallback a inline)
    const clearBtn = document.getElementById('btn-clear-payments');
    if (clearBtn) clearBtn.addEventListener('click', (e) => { e.preventDefault(); clearPaymentFilters(); });

    // Permite pulsar Enter en el filtro de cliente para aplicar
    const clientQ = document.getElementById('payment-client-q');
    if (clientQ) clientQ.addEventListener('keyup', (e) => { if (e.key === 'Enter') filterPayments(); });
});

// Delegación global como respaldo para 'Ver pagos' (por si falla el listener de la tabla)
document.addEventListener('click', (e) => {
    const btn = e.target.closest('button.js-view-payments');
    if (btn) {
        e.preventDefault();
        const id = Number(btn.getAttribute('data-id'));
        viewClientPayments(id);
    }
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
window.filterClients = filterClients;
window.clearClientFilters = clearClientFilters;
window.showPaymentForm = showPaymentForm;
window.clearPaymentFilters = clearPaymentFilters;
window.viewClientPayments = viewClientPayments;
window.voidPayment = voidPayment;

// exponer cargadores del dashboard para que navbar.js delegue correctamente
window.loadDashboardStats = loadDashboardStats;
window.loadRecentActivities = loadRecentActivities;
window.loadReportSummary = loadReportSummary;

// Helper: resuelve múltiples clientIds a partir de un texto (id directo, nombre o email)
function resolveClientIdsFromQuery(q) {
    if (!q) return [];
    const m = q.match(/^\d+/);
    if (m) return [Number(m[0])];
    const needle = q.toLowerCase();
    return (clientsCache || [])
        .filter(c => {
            const name = `${c.firstName || ''} ${c.lastName || ''}`.trim().toLowerCase();
            const email = (c.email || '').toLowerCase();
            return name.includes(needle) || email.includes(needle);
        })
        .map(c => Number(c.id));
}
