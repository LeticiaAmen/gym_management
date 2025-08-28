const BASE_URL = ""; // mismo origen que el backend
const $ = (s, r=document) => r.querySelector(s);

let CLIENTS = [
    { id:1, name:"Ana Pérez", email:"ana@mail.com", phone:"099111111", status:"active" },
    { id:2, name:"Juan García", email:"juan@mail.com", phone:"099222222", status:"paused" }
];

// Render de la tabla + KPIs
function renderClients(list){
    const tbody = $('#clientsTbody');
    tbody.innerHTML = list.map(c => `
    <tr>
      <td>${c.name}</td>
      <td>${c.email}</td>
      <td>${c.phone || '-'}</td>
      <td>${c.status}</td>
      <td>
        <button class="btn" onclick="editClient(${c.id})">Editar</button>
        <button class="btn" onclick="deleteClient(${c.id})">Eliminar</button>
      </td>
    </tr>
  `).join('');

    // KPIs del dashboard
    $('#statTotal').textContent   = list.length;
    $('#statActive').textContent  = list.filter(c => c.status === 'active').length;
    $('#statOverdue').textContent = list.filter(c => c.status === 'overdue').length;
}

// Modal
function openModal(){ $('#clientModal').showModal(); }
function closeModal(){ $('#clientModal').close(); }

// Botones
$('#btnAddClient').onclick = openModal;
$('#btnRefresh').onclick   = () => renderClients(CLIENTS);
$('#closeModal').onclick   = closeModal;

// Alta/edición
$('#clientForm').onsubmit = e => {
    e.preventDefault();
    const id = $('#clientId').value;
    const payload = {
        id: id || Date.now(),
        name:  $('#clientName').value,
        email: $('#clientEmail').value,
        phone: $('#clientPhone').value,
        status: $('#clientStatus').value
    };
    if (id) {
        CLIENTS = CLIENTS.map(c => String(c.id) === String(id) ? payload : c);
    } else {
        CLIENTS.push(payload);
    }
    renderClients(CLIENTS);
    closeModal();
    showToast('Cliente guardado');
};

function editClient(id){
    const c = CLIENTS.find(x => x.id == id);
    if (!c) return;
    $('#clientId').value = c.id;
    $('#clientName').value = c.name;
    $('#clientEmail').value = c.email;
    $('#clientPhone').value = c.phone || '';
    $('#clientStatus').value = c.status;
    $('#clientModalTitle').textContent = 'Editar cliente';
    openModal();
}

function deleteClient(id){
    CLIENTS = CLIENTS.filter(c => c.id != id);
    renderClients(CLIENTS);
    showToast('Cliente eliminado');
}

function showToast(msg){
    const t = $('#toast');
    t.textContent = msg; t.style.display = 'block';
    setTimeout(() => t.style.display = 'none', 2000);
}

// Carga inicial
renderClients(CLIENTS);
