
function initialization() {
    debugger;
    const togglePassword = document.querySelector('.toggle-senha');
    const password = document.querySelector('#senha');

    if (! togglePassword || ! password) {
        console.error({ togglePassword, password });
    }

    togglePassword?.addEventListener('click', function (e) {
        const type = password.getAttribute('type') === 'password' ? 'text' : 'password';
        password.setAttribute('type', type);
        this.classList.toggle('');
    });
}

document.addEventListener("DOMContentLoaded", function() {
    // Your function or code goes here
    console.log("The DOM is fully built!");
    initialization();
});