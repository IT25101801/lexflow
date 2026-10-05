document.addEventListener("DOMContentLoaded", function () {
	var tabbars = document.querySelectorAll(".tabbar");
	for (var i = 0; i < tabbars.length; i++) {
		initTabbar(tabbars[i]);
	}
});

function initTabbar(tabbar) {
	var panelWrapper = tabbar.nextElementSibling;
	var tabs = tabbar.querySelectorAll(".tab");
	var panels = panelWrapper ? panelWrapper.querySelectorAll(".tabpanel") : [];
	var storageKey = tabbar.getAttribute("data-storage-key");
	
	function activateTab(tabName) {
		for (var i = 0; i < tabs.length; i++) {
			tabs[i].setAttribute("aria-selected", tabs[i].getAttribute("data-tab") === tabName ? "true" : "false");
		}
		for (var j = 0; j < panels.length; j++) {
			panels[j].classList.toggle("active", panels[j].getAttribute("data-tab") === tabName);
		}
		if (storageKey) {
			try {
				sessionStorage.setItem(storageKey, tabName);
			} catch (e) {
			}
		}
	}
	
	for (var i = 0; i < tabs.length; i++) {
		tabs[i].addEventListener("click", function () {
			activateTab(this.getAttribute("data-tab"));
		});
	}
	
	if (tabs.length === 0) {
		return;
	}
	
	var savedTab = null;
	if (storageKey) {
		try {
			savedTab = sessionStorage.getItem(storageKey);
		} catch (e) {
		}
	}
	
	var savedTabExists = false;
	for (var k = 0; k < tabs.length; k++) {
		if (tabs[k].getAttribute("data-tab") === savedTab) {
			savedTabExists = true;
			break;
		}
	}
	
	activateTab(savedTabExists ? savedTab : tabs[0].getAttribute("data-tab"));
}
