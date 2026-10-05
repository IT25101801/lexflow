// Shared picker for every multi-file upload (<input type="file" multiple>): lists the chosen
// files under the input so each can be renamed or dropped before the form is sent. Picking
// again adds to the list instead of replacing it. The renamed files are written back into the
// input itself, so the server receives them under the new names without any server-side change.
function setUpFilePicker(input) {
	var picked = [];   // [{file, base, ext}]
	var syncing = false;
	
	var list = document.createElement("div");
	list.className = "file-pick-list";
	input.insertAdjacentElement("afterend", list);
	
	function splitName(filename) {
		var dot = filename.lastIndexOf(".");
		return dot > 0 ? {base: filename.substring(0, dot), ext: filename.substring(dot)} : {base: filename, ext: ""};
	}
	
	function sync() {
		var dataTransfer = new DataTransfer();
		
		picked.forEach(function (entry) {
			var base = entry.base.trim() || splitName(entry.file.name).base;
			dataTransfer.items.add(new File([entry.file], base + entry.ext, {type: entry.file.type, lastModified: entry.file.lastModified}));
		});
		
		syncing = true;
		input.files = dataTransfer.files;
		syncing = false;
	}
	
	function render() {
		list.innerHTML = "";
		
		picked.forEach(function (entry, index) {
			var row = document.createElement("div");
			row.className = "multi-input-row file-pick-row";
			
			var name = document.createElement("input");
			name.type = "text";
			name.value = entry.base;
			name.setAttribute("aria-label", "File name");
			name.addEventListener("input", function () {
				entry.base = name.value;
				sync();
			});
			
			var ext = document.createElement("span");
			ext.className = "file-pick-ext";
			ext.textContent = entry.ext;
			
			var remove = document.createElement("button");
			remove.type = "button";
			remove.className = "multi-input-remove";
			remove.title = "Remove file";
			remove.textContent = "✕";
			remove.addEventListener("click", function () {
				picked.splice(index, 1);
				sync();
				render();
			});
			
			row.appendChild(name);
			row.appendChild(ext);
			row.appendChild(remove);
			list.appendChild(row);
		});
	}
	
	input.addEventListener("change", function () {
		if (syncing) return;
		
		Array.prototype.forEach.call(input.files, function (file) {
			var parts = splitName(file.name);
			picked.push({file: file, base: parts.base, ext: parts.ext});
		});
		
		sync();
		render();
	});
	
	if (input.form) {
		input.form.addEventListener("reset", function () {
			picked = [];
			render();
		});
	}
}

document.addEventListener("DOMContentLoaded", function () {
	document.querySelectorAll("input[type=\"file\"][multiple]").forEach(setUpFilePicker);
});
