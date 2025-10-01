document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    // Si hay un token en la URL, lo guardamos en el campo oculto
    if (token) {
        document.getElementById('token').value = token;
        // También verificamos si el token es válido
        validateToken(token);
    } else {
        // Si no hay token, mostramos un error y deshabilitamos el formulario
        showError('Token no válido o expirado. Por favor solicita un nuevo enlace de recuperación.');
        document.querySelector('button[type="submit"]').disabled = true;
    }

    const form = document.getElementById('resetPasswordForm');
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        resetPassword();
    });
});

/**
 * Verifica si el token es válido antes de permitir el cambio de contraseña
 */
function validateToken(token) {
    fetch(`/api/password/validate-token/${token}`)
        .then(response => response.json())
        .then(isValid => {
            if (!isValid) {
                showError('El enlace ha expirado o no es válido. Por favor solicita un nuevo enlace de recuperación.');
                document.querySelector('button[type="submit"]').disabled = true;
            }
        })
        .catch(error => {
            console.error('Error al validar el token:', error);
            showError('Error al validar el enlace. Por favor intenta nuevamente más tarde.');
            document.querySelector('button[type="submit"]').disabled = true;
        });
}

/**
 * Envía la solicitud para cambiar la contraseña
 */
function resetPassword() {
    const token = document.getElementById('token').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    // Validación básica
    if (newPassword !== confirmPassword) {
        showError('Las contraseñas no coinciden');
        return;
    }

    // Validar requisitos de la contraseña
    const passwordRegex = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$/;
    if (!passwordRegex.test(newPassword)) {
        showError('La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número');
        return;
    }

    // Enviar solicitud al servidor
    fetch('/api/password/confirm-reset', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            token: token,
            newPassword: newPassword,
            confirmPassword: confirmPassword
        })
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(text) });
        }
        return response.text();
    })
    .then(message => {
        showMessage(message);
        document.getElementById('newPassword').value = '';
        document.getElementById('confirmPassword').value = '';
        document.querySelector('button[type="submit"]').disabled = true;

        // Redireccionar al login después de 3 segundos
        setTimeout(() => {
            window.location.href = '/';
        }, 3000);
    })
    .catch(error => {
        showError(error.message || 'Error al restablecer la contraseña');
    });
}

function showMessage(text) {
    const messageEl = document.getElementById('message');
    messageEl.textContent = text;
    messageEl.classList.add('success');
    messageEl.classList.remove('hidden');
}

function showError(text) {
    const errorEl = document.getElementById('error');
    errorEl.textContent = text;
    errorEl.classList.remove('hidden');
}
