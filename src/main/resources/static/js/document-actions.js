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
