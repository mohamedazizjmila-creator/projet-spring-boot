// Confirmation avant suppression
document.addEventListener('DOMContentLoaded', function() {
    // Confirmation pour les boutons supprimer
    const deleteButtons = document.querySelectorAll('.btn-danger');
    deleteButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            if (!confirm('Êtes-vous sûr de vouloir supprimer ?')) {
                e.preventDefault();
            }
        });
    });
    
    // Formatage des prix
    const priceCells = document.querySelectorAll('td:nth-child(3)');
    priceCells.forEach(cell => {
        const price = parseFloat(cell.textContent);
        if (!isNaN(price)) {
            cell.textContent = price.toFixed(2) + ' €';
        }
    });
});