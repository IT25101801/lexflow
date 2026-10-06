function toggleDocDelete(documentId) {
	var checkbox = document.getElementById("doc-del-" + documentId);
	var row = document.getElementById("doc-row-" + documentId);
	var button = document.getElementById("doc-del-btn-" + documentId);
	
	checkbox.checked = !checkbox.checked;
	row.classList.toggle("marked-for-deletion", checkbox.checked);
	
	if (checkbox.checked) {
		button.textContent = "Undo";
		button.classList.remove("action-btn-delete");
		button.classList.add("action-btn-edit");
	} else {
		button.textContent = "Delete";
		button.classList.remove("action-btn-edit");
		button.classList.add("action-btn-delete");
	}
}

document.addEventListener("DOMContentLoaded", function () {
	var forms = document.querySelectorAll("form");
	
	for (var i = 0; i < forms.length; i++) {
		forms[i].addEventListener("submit", confirmDocumentDeletes);
	}
	
});

function confirmDocumentDeletes(event) {
	var marked = event.target.querySelectorAll("input[id^='doc-del-']:checked").length;
	
	if (marked > 0 && !confirm("Delete " + marked + (marked === 1 ? " document" : " documents") + "? This action cannot be undone.")) {
		event.preventDefault();
		event.stopImmediatePropagation();
	}
	
}
