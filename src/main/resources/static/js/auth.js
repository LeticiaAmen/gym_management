document.getElementById('loginForm').addEventListener('submit', async (e) =>{

    // 1) Previene el comportamiento por defecto del formulario (recargar la página).
    e.preventDefault();

    // 2) Obtiene los valores de email y password desde los inputs del formulario.
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    // 3) Obtiene el elemento donde se muestran errores y lo limpia.
    const errorElem = document.getElementById('error');
    errorElem.textContent = '';

    try {
        // 4) Envía una petición POST al endpoint de login de la API (/auth/login).
        //    Pasa las credenciales en formato JSON en el body.
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        // 5) Si la respuesta es OK (200), significa que el login fue exitoso.
        if (response.ok) {
            // 5.1) Extrae el token de la respuesta JSON.
            const { token } = await response.json();
            // 5.2) Guarda el token en localStorage para usarlo en futuras peticiones.
            localStorage.setItem('token', token);
            // 5.3) Redirige al dashboard del administrador.
            window.location.href = '/admin/dashboard.html';
        } else {
            // 6) Si la respuesta no fue exitosa, muestra el mensaje de error
            //    devuelto por el servidor o un mensaje genérico.
            const message = await response.text();
            errorElem.textContent = message || 'Error al iniciar sesión';
        }
    } catch (err) {
        // 7) Si ocurre un error de red (por ejemplo, el backend no responde),
        //    muestra un mensaje genérico al usuario.
        errorElem.textContent = 'Error de red';
    }
});
