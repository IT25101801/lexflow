function addMultiInputRow(containerId, inputName, placeholder) {
	var container = document.getElementById(containerId);
	var row = document.createElement("div");
	row.className = "multi-input-row";
	
	var input = document.createElement("input");
	input.type = "text";
	input.name = inputName;
	if (placeholder) {
		input.placeholder = placeholder;
	}
	
	var removeButton = document.createElement("button");
	removeButton.type = "button";
	removeButton.className = "multi-input-remove";
	removeButton.textContent = "Remove";
	removeButton.onclick = function () {
		row.remove();
	};
	
	row.appendChild(input);
	row.appendChild(removeButton);
	container.appendChild(row);
}
