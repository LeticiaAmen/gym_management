export async function apiFetch(path, options = {}) {
    //Permite desactivar redirección en 401 para manejar el error en el caller
    const { noRedirectOn401, ...rest } = options;

    //Recupera el token JWT almacenado en el navegador (localStorage)
    // este token se guardó previamente cuando el usuario inició sesión
    const token = localStorage.getItem('token');

    // clona el objeto de opciones que recibe la función (para no modificar el original).
    const opts = { ...rest};

    //Garantiza que haya un objeto de headers en las opciones.
    // si ya existen headers, se mantiene y se copian
    opts.headers = { ...(opts.headers || {}) };

    // si hay un token almacenado, lo agrega al header autorization
    if (token) {
        opts.headers['Authorization'] = `Bearer ${token}`;
    }

    //Ejecuta la llamada HTTP usando fetch con la ruta y las opciones.
    const response = await fetch(path, opts);

    // si el servidor responde con 401 unauthorized y no se desactivó la redirección,
    // significa que el token no es válido o expiró → volver al login.
    if (response.status === 401 && !noRedirectOn401) {
        window.location.href = '/index.html';
    }
    //Devuelve la respuesta completa (no parsea JSON, eso se hace fuera)
    return response;
}

/**
 * Registra un nuevo administrador en el sistema.
 *
 * Esta función envía una solicitud POST al endpoint /auth/admin/register con los datos
 * del nuevo administrador. Solo usuarios con rol ADMIN pueden realizar esta operación.
 *
 * @param {Object} adminData - Datos del administrador a registrar
 * @param {string} adminData.email - Correo electrónico del nuevo administrador
 * @param {string} adminData.password - Contraseña elegida (se enviará encriptada por HTTPS)
 * @param {string} adminData.confirmPassword - Confirmación de la contraseña
 * @returns {Promise<Response>} - Respuesta del servidor
 */
export async function registerAdmin(adminData) {
    // Verificamos que las contraseñas coincidan antes de enviar al servidor
    if (adminData.password !== adminData.confirmPassword) {
        throw new Error('Las contraseñas no coinciden');
    }

    return apiFetch('/auth/admin/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(adminData)
    });
}

/**
 * Obtiene la lista de todos los administradores registrados en el sistema.
 *
 * Esta función realiza una solicitud GET al endpoint /api/admins para obtener
 * la lista completa de administradores. Solo accesible por usuarios con rol ADMIN.
 *
 * @returns {Promise<Array>} - Lista de administradores en formato DTO
 */
export async function getAdmins() {
    const response = await apiFetch('/api/admins');
    if (!response.ok) {
        throw new Error('No se pudieron cargar los administradores');
    }
    return response.json();
}

/**
 * Cambia la contraseña del usuario administrador actualmente autenticado.
 *
 * Esta función envía una solicitud POST al endpoint /api/users/change-password con los datos
 * de la contraseña actual y la nueva contraseña para realizar el cambio.
 *
 * @param {Object} passwordData - Datos para el cambio de contraseña
 * @param {string} passwordData.currentPassword - Contraseña actual para validación
 * @param {string} passwordData.newPassword - Nueva contraseña
 * @param {string} passwordData.confirmPassword - Confirmación de la nueva contraseña
 * @returns {Promise<Response>} - Respuesta del servidor
 */
export async function changePassword(passwordData) {
    return apiFetch('/api/users/change-password', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(passwordData)
    });
}
