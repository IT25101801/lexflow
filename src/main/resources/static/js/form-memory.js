// form-memory.js – every form is checked by the server (the DTOs), not the browser. When a form
// comes back with an error, the values the user typed are put back and its modal or tab reopened.

var FORM_MEMORY_KEY = "formMemory";
var FORM_MEMORY_MAX_AGE = 120000;
var SKIPPED_INPUT_TYPES = ["hidden", "password", "file", "submit", "button", "reset"];

document.addEventListener("DOMContentLoaded", function () {
	var forms = document.querySelectorAll("form");

	for (var i = 0; i < forms.length; i++) {
		forms[i].noValidate = true;
		forms[i].addEventListener("submit", saveFormValues);
	}

	restoreFormValues();
});

function formMemoryKey(form) {
	return form.id || form.getAttribute("action") || "";
}

function saveFormValues(event) {
	var form = event.target;

	if (form.method.toLowerCase() !== "post") {
		return;
	}

	var values = [];

	for (var i = 0; i < form.elements.length; i++) {
		var element = form.elements[i];

		if (!element.name || SKIPPED_INPUT_TYPES.indexOf(element.type) !== -1) {
			continue;
		}

		if (element.type === "checkbox" || element.type === "radio") {
			values.push({name: element.name, value: element.value, checked: element.checked});
		} else {
			values.push({name: element.name, value: element.value});
		}

	}

	try {
		sessionStorage.setItem(FORM_MEMORY_KEY, JSON.stringify({key: formMemoryKey(form), action: form.action, values: values, savedAt: Date.now()}));
	} catch (e) {
	}

}

function restoreFormValues() {
	var saved = null;

	try {
		saved = JSON.parse(sessionStorage.getItem(FORM_MEMORY_KEY));
		sessionStorage.removeItem(FORM_MEMORY_KEY);
	} catch (e) {
		return;
	}

	if (!saved || Date.now() - saved.savedAt > FORM_MEMORY_MAX_AGE) {
		return;
	}
	
	restorePageUrl();
	
	if (!document.querySelector(".flash-error")) {
		return;
	}

	var form = findSavedForm(saved);

	if (!form) {
		return;
	}

	form.action = saved.action;
	var valuesByName = {};

	for (var i = 0; i < saved.values.length; i++) {
		var savedValue = saved.values[i];

		if (!valuesByName[savedValue.name]) {
			valuesByName[savedValue.name] = [];
		}

		valuesByName[savedValue.name].push(savedValue);
	}

	for (var name in valuesByName) {
		restoreField(form, name, valuesByName[name]);
	}

	reopenFormContainer(form);
}

function findSavedForm(saved) {
	var forms = document.querySelectorAll("form");
	var match = null;

	for (var i = 0; i < forms.length; i++) {

		if (formMemoryKey(forms[i]) !== saved.key) {
			continue;
		}

		if (forms[i].action === saved.action) {
			return forms[i];
		}

		if (!match) {
			match = forms[i];
		}

	}

	return match;
}

function restoreField(form, name, savedValues) {
	var fields = fieldsNamed(form, name);

	if (fields.length === 0) {
		return;
	}

	if (fields[0].type === "checkbox" || fields[0].type === "radio") {

		for (var i = 0; i < fields.length; i++) {

			for (var j = 0; j < savedValues.length; j++) {

				if (savedValues[j].value === fields[i].value) {
					fields[i].checked = savedValues[j].checked;
				}

			}

			fields[i].dispatchEvent(new Event("change", {bubbles: true}));
		}

		return;
	}

	var row = fields[0].closest(".multi-input-row");

	while (row && row.parentElement.id && fields.length < savedValues.length) {
		addMultiInputRow(row.parentElement.id, name, fields[0].placeholder);
		fields = fieldsNamed(form, name);
	}

	for (var k = 0; k < fields.length && k < savedValues.length; k++) {
		fields[k].value = savedValues[k].value;
		fields[k].dispatchEvent(new Event("input", {bubbles: true}));
		fields[k].dispatchEvent(new Event("change", {bubbles: true}));
	}

}

function fieldsNamed(form, name) {
	var fields = [];

	for (var i = 0; i < form.elements.length; i++) {

		if (form.elements[i].name === name && SKIPPED_INPUT_TYPES.indexOf(form.elements[i].type) === -1) {
			fields.push(form.elements[i]);
		}

	}

	return fields;
}

function reopenFormContainer(form) {
	var overlay = form.closest(".modal-overlay");

	if (overlay) {
		overlay.style.display = "flex";
	}

	var panel = form.closest(".tabpanel");

	if (panel && panel.parentElement && panel.parentElement.parentElement) {
		var tab = panel.parentElement.parentElement.querySelector(".tab[data-tab='" + panel.getAttribute("data-tab") + "']");

		if (tab) {
			tab.click();
		}

	}

}

document.addEventListener("DOMContentLoaded", function () {
	var today = new Date();
	var minimum = today.getFullYear() + "-" + String(today.getMonth() + 1).padStart(2, "0") + "-" + String(today.getDate()).padStart(2, "0");
	var dateInputs = document.querySelectorAll("input[type='date'][data-not-past]");
	
	for (var i = 0; i < dateInputs.length; i++) {
		dateInputs[i].min = minimum;
	}
	
	var adultBirthDate = new Date();
	adultBirthDate.setFullYear(adultBirthDate.getFullYear() - 18);
	var maximum = adultBirthDate.getFullYear() + "-" + String(adultBirthDate.getMonth() + 1).padStart(2, "0") + "-" + String(adultBirthDate.getDate()).padStart(2, "0");
	var adultBirthDateInputs = document.querySelectorAll("input[type='date'][data-adult-birth-date]");
	
	for (var j = 0; j < adultBirthDateInputs.length; j++) {
		adultBirthDateInputs[j].max = maximum;
	}
	
});

// After a form is sent the server renders the page at the action's address (…/cases/1/tasks/5/edit-title);
// put the page's own address back so a refresh reloads the page instead of sending the form again.
function restorePageUrl() {
	var path = window.location.pathname;
	var match = path.match(/^(\/[^\/]+\/[^\/]+\/\d+)\/.+$/) || path.match(/^(\/[^\/]+\/profile)\/.+$/);
	
	if (match) {
		history.replaceState(null, "", match[1] + window.location.hash);
	}
	
}

document.addEventListener("DOMContentLoaded", function () {
	var flashes = document.querySelectorAll(".flash");
	
	for (var i = 0; i < flashes.length; i++) {
		hideFlashLater(flashes[i], flashes[i].classList.contains("flash-error") ? 4000 : 2000);
	}
	
});

function hideFlashLater(flash, delay) {
	setTimeout(function () {
		flash.style.transition = "opacity 0.4s ease";
		flash.style.opacity = "0";
		
		setTimeout(function () {
			flash.remove();
		}, 400);
	}, delay);
}
