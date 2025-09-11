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