/* Lumora front-end behaviour.
   Loaded as an external file because the Content-Security-Policy forbids inline
   script. Everything here is progressive: the pages work with JavaScript off. */

(function () {
    "use strict";

    /* ---- Theme -------------------------------------------------------- */

    var STORAGE_KEY = "lumora.theme";

    function applyTheme(theme) {
        document.documentElement.setAttribute("data-theme", theme);
        var button = document.getElementById("themeToggle");
        if (button) {
            button.textContent = theme === "dark" ? "☀" : "☽";
            button.setAttribute("aria-label", theme === "dark" ? "Switch to light theme" : "Switch to dark theme");
        }
    }

    function initTheme() {
        var stored = null;
        try {
            stored = window.localStorage.getItem(STORAGE_KEY);
        } catch (e) {
            /* Private browsing: fall back to the light theme. */
        }
        applyTheme(stored === "dark" ? "dark" : "light");

        var button = document.getElementById("themeToggle");
        if (!button) {
            return;
        }
        button.addEventListener("click", function () {
            var next = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
            applyTheme(next);
            try {
                window.localStorage.setItem(STORAGE_KEY, next);
            } catch (e) {
                /* Ignore: the theme simply will not persist. */
            }
        });
    }

    /* ---- Sidebar on small screens ------------------------------------- */

    function initSidebar() {
        var toggle = document.getElementById("sidebarToggle");
        var sidebar = document.getElementById("sidebar");
        if (!toggle || !sidebar) {
            return;
        }
        toggle.addEventListener("click", function () {
            sidebar.classList.toggle("open");
        });
        document.addEventListener("click", function (event) {
            if (window.innerWidth > 980 || !sidebar.classList.contains("open")) {
                return;
            }
            if (!sidebar.contains(event.target) && event.target !== toggle) {
                sidebar.classList.remove("open");
            }
        });
    }

    /* ---- Client-side table filtering ---------------------------------- */

    function initTableFilters() {
        var inputs = document.querySelectorAll("[data-filter]");
        Array.prototype.forEach.call(inputs, function (input) {
            var table = document.getElementById(input.getAttribute("data-filter"));
            if (!table) {
                return;
            }
            input.addEventListener("input", function () {
                var needle = input.value.trim().toLowerCase();
                var rows = table.tBodies.length ? table.tBodies[0].rows : [];
                var shown = 0;

                Array.prototype.forEach.call(rows, function (row) {
                    if (row.hasAttribute("data-no-filter")) {
                        return;
                    }
                    var match = needle === "" || row.textContent.toLowerCase().indexOf(needle) !== -1;
                    row.style.display = match ? "" : "none";
                    if (match) {
                        shown += 1;
                    }
                });

                var counter = document.querySelector('[data-filter-count="' + table.id + '"]');
                if (counter) {
                    counter.textContent = shown + (shown === 1 ? " record" : " records");
                }
            });
        });
    }

    /* ---- Bulk register / mark sheet helpers --------------------------- */

    function initBulkSetters() {
        var buttons = document.querySelectorAll("[data-set-all]");
        Array.prototype.forEach.call(buttons, function (button) {
            button.addEventListener("click", function () {
                var value = button.getAttribute("data-set-all");
                var scope = document.getElementById(button.getAttribute("data-scope"));
                if (!scope) {
                    return;
                }
                var selects = scope.querySelectorAll("select[data-register]");
                Array.prototype.forEach.call(selects, function (select) {
                    select.value = value;
                });
            });
        });
    }

    /* ---- Confirm destructive submits ---------------------------------- */

    function initConfirms() {
        var forms = document.querySelectorAll("form[data-confirm]");
        Array.prototype.forEach.call(forms, function (form) {
            form.addEventListener("submit", function (event) {
                if (!window.confirm(form.getAttribute("data-confirm"))) {
                    event.preventDefault();
                }
            });
        });
    }

    /* ---- Dismissable alerts ------------------------------------------- */

    function initAlerts() {
        var alerts = document.querySelectorAll(".alert[data-autohide]");
        Array.prototype.forEach.call(alerts, function (alert) {
            window.setTimeout(function () {
                alert.style.transition = "opacity .4s ease";
                alert.style.opacity = "0";
                window.setTimeout(function () {
                    alert.remove();
                }, 400);
            }, 6000);
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        initTheme();
        initSidebar();
        initTableFilters();
        initBulkSetters();
        initConfirms();
        initAlerts();
    });

    // Apply the stored theme before first paint to avoid a flash of light theme.
    initTheme();
}());
