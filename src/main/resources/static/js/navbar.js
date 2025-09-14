// Navbar functionality
class Navbar {
    constructor() {
        this.init();
    }

    init() {
        this.setupActiveLink();
        this.setupDropdowns();
        this.setupUserInfo();
        this.setupSectionNavigation();
    }

    // Marca el enlace activo basado en la URL actual o sección
    setupActiveLink() {
        const navLinks = document.querySelectorAll('.navbar-nav a');
        const currentPath = window.location.pathname;

        navLinks.forEach(link => {
            if (link.getAttribute('href') === currentPath) {
                link.classList.add('active');
            }
        });
    }

    // Maneja la navegación entre secciones en el dashboard
    setupSectionNavigation() {
        const navLinks = document.querySelectorAll('.navbar-nav a[data-section]');

        navLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();

                // Remover clase active de todos los enlaces
                navLinks.forEach(l => l.classList.remove('active'));

                // Añadir clase active al enlace clickeado
                link.classList.add('active');

                // Obtener la sección del data-section attribute
                const section = link.getAttribute('data-section');

                // Mostrar la sección correspondiente
                if (section === 'dashboard') {
                    this.showDashboard();
                } else if (typeof window.showSection === 'function') {
                    window.showSection(section);
                } else {
                    // Si showSection no existe, manejamos la navegación aquí
                    this.showSectionFallback(section);
                }
            });
        });
    }

    // Mostrar el dashboard principal
    showDashboard() {
        // Ocultar todas las secciones
        const sections = document.querySelectorAll('.section');
        sections.forEach(section => {
            section.style.display = 'none';
        });

        // Mostrar solo el dashboard
        const dashboardSection = document.getElementById('dashboard-section');
        if (dashboardSection) {
            dashboardSection.style.display = 'block';
        }

        // Actualizar estadísticas del dashboard
        this.updateDashboardStats();
    }

    // Fallback para mostrar secciones si showSection no existe
    showSectionFallback(sectionName) {
        // Ocultar todas las secciones
        const sections = document.querySelectorAll('.section');
        sections.forEach(section => {
            section.style.display = 'none';
        });

        // Mostrar la sección correspondiente
        const targetSection = document.getElementById(sectionName + '-section');
        if (targetSection) {
            targetSection.style.display = 'block';
        }
    }

    // Configura los dropdowns del navbar
    setupDropdowns() {
        const dropdowns = document.querySelectorAll('.dropdown');

        dropdowns.forEach(dropdown => {
            const toggle = dropdown.querySelector('.dropdown-toggle');
            const menu = dropdown.querySelector('.dropdown-menu');

            if (toggle && menu) {
                toggle.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();

                    // Cerrar otros dropdowns
                    document.querySelectorAll('.dropdown-menu.show').forEach(otherMenu => {
                        if (otherMenu !== menu) {
                            otherMenu.classList.remove('show');
                        }
                    });

                    // Toggle del dropdown actual
                    menu.classList.toggle('show');
                });
            }
        });

        // Cerrar dropdowns al hacer clic fuera
        document.addEventListener('click', () => {
            document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
                menu.classList.remove('show');
            });
        });
    }

    // Configura la información del usuario
    setupUserInfo() {
        const userInfo = this.getUserInfo();
        const userSpan = document.querySelector('.navbar-user span');

        if (userSpan && userInfo) {
            userSpan.textContent = `Bienvenido, ${userInfo.name || userInfo.email}`;
        }
    }

    // Obtiene información del usuario desde localStorage o sessionStorage
    getUserInfo() {
        try {
            const token = localStorage.getItem('token') || sessionStorage.getItem('token');
            if (token) {
                // Decodificar token JWT si es necesario
                const payload = JSON.parse(atob(token.split('.')[1]));
                return payload;
            }
        } catch (error) {
            console.error('Error al obtener información del usuario:', error);
        }
        return null;
    }

    // Función para cerrar sesión
    logout() {
        if (confirm('¿Estás seguro que deseas cerrar sesión?')) {
            localStorage.removeItem('token');
            sessionStorage.removeItem('token');
            localStorage.removeItem('user');
            sessionStorage.removeItem('user');
            window.location.href = '/';
        }
    }

    // Actualizar estadísticas del dashboard
    updateDashboardStats() {
        // Aquí puedes hacer llamadas a la API para obtener datos reales
        // Por ahora mostramos datos de ejemplo

        // Simular carga de datos
        setTimeout(() => {
            const activeClientsEl = document.getElementById('active-clients-count');
            const monthlyRevenueEl = document.getElementById('monthly-revenue');
            const pendingPaymentsEl = document.getElementById('pending-payments-count');

            if (activeClientsEl) activeClientsEl.textContent = '234';
            if (monthlyRevenueEl) monthlyRevenueEl.textContent = '$12,450';
            if (pendingPaymentsEl) pendingPaymentsEl.textContent = '18';
        }, 100);
    }
}

// Función global para cerrar sesión (compatible con onclick)
function logout() {
    const navbar = new Navbar();
    navbar.logout();
}

// Función global para que otras partes de la app puedan actualizar el navbar
function updateNavbarActiveSection(sectionName) {
    Navbar.setActiveSection(sectionName);
}

// Interceptar las llamadas a showSection existentes para mantener el navbar actualizado
if (typeof window.showSection === 'function') {
    const originalShowSection = window.showSection;
    window.showSection = function(section) {
        // Llamar a la función original
        originalShowSection(section);
        // Actualizar el navbar
        updateNavbarActiveSection(section);
    };
}

// Inicializar navbar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    const navbar = new Navbar();

    // Si estamos en el dashboard, marcar la primera sección como activa por defecto
    if (window.location.pathname.includes('dashboard')) {
        const firstNavLink = document.querySelector('.navbar-nav a[data-section="dashboard"]');
        if (firstNavLink) {
            firstNavLink.classList.add('active');
        }
    }
});
