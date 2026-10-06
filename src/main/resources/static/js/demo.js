// demo.js – pre-fills forms with sample data for demo purposes.

function fillLogin(email, password) {
	document.getElementById("username").value = email;
	document.getElementById("password").value = password;
}

// Multi-input-row lists: remove every extra row a previous fill added, then fill the first row.
function fillMultiInputRow(containerId, fieldName, value) {
	let inputs = document.querySelectorAll("#" + containerId + " input[name='" + fieldName + "']");
	
	if (inputs.length === 0) {
		addMultiInputRow(containerId, fieldName);
		inputs = document.querySelectorAll("#" + containerId + " input[name='" + fieldName + "']");
	}
	
	for (let i = 1; i < inputs.length; i++) {
		const row = inputs[i].closest(".multi-input-row");
		if (row) row.remove();
	}
	
	if (inputs.length > 0) inputs[0].value = value;
}

// Adds a second row (via the page's own addMultiInputRow, from multi-input.js) and fills it.
function addAndFillMultiInputRow(containerId, fieldName, placeholder, value) {
	addMultiInputRow(containerId, fieldName, placeholder);
	const inputs = document.querySelectorAll("#" + containerId + " input[name='" + fieldName + "']");
	inputs[inputs.length - 1].value = value;
}

// A real <input type="file"> can't be set via input.value for security reasons, so demo
// buttons fetch a sample PDF already sitting in static resources and hand it to the input
// as an actual File object through a DataTransfer, the same way a browser would after a user
// picks a file. Fire-and-forget: the caller doesn't await this.
// Demo fills replace whatever the file field holds; file-picker.js would otherwise add to its list on every press.
function setDemoFiles(input, files) {
	const dataTransfer = new DataTransfer();
	
	for (let i = 0; i < files.length; i++) {
		dataTransfer.items.add(files[i]);
	}
	
	input.dataset.replacePicked = "true";
	input.files = dataTransfer.files;
	input.dispatchEvent(new Event("change", {bubbles: true}));
}

function clearFileInput(inputId) {
	setDemoFiles(document.getElementById(inputId), []);
}

function fillLargeFileInput(inputId, filename) {
	setDemoFiles(document.getElementById(inputId), [new File([new Uint8Array(51 * 1024 * 1024)], filename, {type: "application/pdf"})]);
}

async function fillFileInput(inputId, url, filename) {
	const input = document.getElementById(inputId);
	if (!input) return;
	const response = await fetch(url);
	const blob = await response.blob();
	setDemoFiles(input, [new File([blob], filename, {type: "application/pdf"})]);
}

function fillClientRegisterFormB1() {
	fillClientRegisterFormB2();
	document.querySelector("#phoneNumberRows input[name='phoneNumbers']").value = "07610";
	document.getElementById("nicNumber").value = "93587V";
}

function fillClientRegisterFormB2() {
	// Nimali Karunatilake — a new client, not yet in the system. Two phone numbers.
	document.getElementById("email").value = "nimali.karunatilake@gmail.com";
	document.getElementById("password").value = "nimali.karunatilake@gmail.com";
	document.getElementById("name").value = "Nimali Karunatilake";
	document.getElementById("dateOfBirth").value = "1993-03-28";
	document.getElementById("gender").value = "FEMALE";
	fillMultiInputRow("addressRows", "addresses", "No. 60, Duplication Road, Colombo");
	fillMultiInputRow("phoneNumberRows", "phoneNumbers", "0761000006");
	addAndFillMultiInputRow("phoneNumberRows", "phoneNumbers", "0712345678", "0711000006");
	document.getElementById("nicNumber").value = "935876789V";
	fillFileInput("nicFile", "/demo-files/nic-document.pdf", "NIC Document.pdf");
}

function fillClientRegisterFormD() {
	// Dinusha Rathnayake — a second new client, so two people can register without resetting the database.
	document.getElementById("email").value = "dinusha.rathnayake@gmail.com";
	document.getElementById("password").value = "dinusha.rathnayake@gmail.com";
	document.getElementById("name").value = "Dinusha Rathnayake";
	document.getElementById("dateOfBirth").value = "1996-11-02";
	document.getElementById("gender").value = "FEMALE";
	fillMultiInputRow("addressRows", "addresses", "No. 14, Kandy Road, Kurunegala");
	fillMultiInputRow("phoneNumberRows", "phoneNumbers", "0761000008");
	document.getElementById("nicNumber").value = "968071234V";
}

function fillClientRegisterFormD1() {
	fillClientRegisterFormD();
	clearFileInput("nicFile");
}

function fillClientRegisterFormD2() {
	fillClientRegisterFormD();
	fillFileInput("nicFile", "/demo-files/nic-document.pdf", "NIC Document.pdf");
}

function fillWalkinClientRegisterFormB1() {
	fillWalkinClientRegisterFormB2();
	document.querySelector("#phoneNumberRows input[name='phoneNumbers']").value = "07610";
	document.getElementById("nicNumber").value = "91195V";
}

function fillWalkinClientRegisterFormB2() {
	// Kasun Athukorala — a different new client, for the Client Relations Officer's walk-in registration form. Two addresses.
	document.getElementById("email").value = "kasun.athukorala@gmail.com";
	document.getElementById("password").value = "kasun.athukorala@gmail.com";
	document.getElementById("name").value = "Kasun Athukorala";
	document.getElementById("dateOfBirth").value = "1991-07-14";
	document.getElementById("gender").value = "MALE";
	fillMultiInputRow("addressRows", "addresses", "No. 5, Bagatalle Road, Colombo");
	addAndFillMultiInputRow("addressRows", "addresses", "No. 123, Anywhere Street, Anywhere City", "No. 22, Havelock Road, Colombo");
	fillMultiInputRow("phoneNumberRows", "phoneNumbers", "0761000007");
	document.getElementById("nicNumber").value = "911957890V";
	fillFileInput("nicFile", "/demo-files/nic-document.pdf", "NIC Document.pdf");
}

function fillWalkinClientRegisterFormD() {
	// Dilan Jayasekara — a second walk-in client, so two people can register without resetting the database.
	document.getElementById("email").value = "dilan.jayasekara@gmail.com";
	document.getElementById("password").value = "dilan.jayasekara@gmail.com";
	document.getElementById("name").value = "Dilan Jayasekara";
	document.getElementById("dateOfBirth").value = "1989-05-19";
	document.getElementById("gender").value = "MALE";
	fillMultiInputRow("addressRows", "addresses", "No. 7, Galle Road, Mount Lavinia");
	fillMultiInputRow("phoneNumberRows", "phoneNumbers", "0761000009");
	document.getElementById("nicNumber").value = "891392345V";
}

function fillWalkinClientRegisterFormD1() {
	fillWalkinClientRegisterFormD();
	clearFileInput("nicFile");
}

function fillWalkinClientRegisterFormD2() {
	fillWalkinClientRegisterFormD();
	fillFileInput("nicFile", "/demo-files/nic-document.pdf", "NIC Document.pdf");
}

function fillRegisterStaffRole(role, data) {
	const roleSelect = document.getElementById("role");
	roleSelect.value = role;
	roleSelect.dispatchEvent(new Event("change"));
	document.getElementById("email").value = data.email;
	document.getElementById("password").value = data.email;
	document.getElementById("name").value = data.name;
	document.getElementById("dateOfBirth").value = data.dateOfBirth;
	document.getElementById("gender").value = data.gender;
	fillMultiInputRow("addressRows", "addresses", data.address);
	fillMultiInputRow("phoneNumberRows", "phoneNumbers", data.phoneNumber);
	document.getElementById("nicNumber").value = data.nicNumber;
	fillFileInput("nicFile", "/demo-files/nic-document.pdf", "NIC Document.pdf");
	if (data.barNumber) {
		document.getElementById("barNumber").value = data.barNumber;
		fillFileInput("barFile", "/demo-files/bar-document.pdf", "Bar Document.pdf");
	} else {
		document.getElementById("barNumber").value = "";
		clearFileInput("barFile");
	}
}

function fillRegisterStaffPartner() {
	// Manjula Ratwatte — a new partner, not yet in the system
	fillRegisterStaffRole("PARTNER", {
		email: "manjula.ratwatte@lexflow.lk", name: "Manjula Ratwatte", dateOfBirth: "1976-04-11",
		gender: "MALE", address: "No. 21, Ward Place, Colombo", phoneNumber: "0761000001",
		nicNumber: "761011234V", barNumber: "0000000021"
	});
}

function fillRegisterStaffAssociate() {
	// Dilshan Weeraratne — a new associate, not yet in the system
	fillRegisterStaffRole("ASSOCIATE", {
		email: "dilshan.weeraratne@lexflow.lk", name: "Dilshan Weeraratne", dateOfBirth: "1990-08-23",
		gender: "MALE", address: "No. 34, Flower Road, Colombo", phoneNumber: "0761000002",
		nicNumber: "902352345V", barNumber: "0000000022"
	});
}

function fillRegisterStaffParalegal() {
	// Sanduni Amerasekera — a new paralegal, not yet in the system
	fillRegisterStaffRole("PARALEGAL", {
		email: "sanduni.amerasekera@lexflow.lk", name: "Sanduni Amerasekera", dateOfBirth: "1995-01-17",
		gender: "FEMALE", address: "No. 12, Barnes Place, Colombo", phoneNumber: "0761000003",
		nicNumber: "955173456V"
	});
}

function fillRegisterStaffClientRelationsOfficer() {
	// Nayomi Gunatilake — a new client relations officer, not yet in the system
	fillRegisterStaffRole("CLIENT_RELATIONS_OFFICER", {
		email: "nayomi.gunatilake@lexflow.lk", name: "Nayomi Gunatilake", dateOfBirth: "1992-06-05",
		gender: "FEMALE", address: "No. 47, Thurstan Road, Colombo", phoneNumber: "0761000004",
		nicNumber: "926564567V"
	});
}

function fillPaymentFormT1() {
	fillPaymentFormT2();
	document.getElementById("cardNumberDisplay").value = "1234 5678 9012";
	document.getElementById("cardNumber").value = "123456789012";
	document.getElementById("card-preview").textContent = "1234 5678 9012";
	document.getElementById("expiry").value = "13/29";
	document.getElementById("expiry-preview").textContent = "13/29";
}

function fillPaymentFormT2() {
	// Demo card — passes the 16-digit/MM-YY/3-digit CVV format checks.
	const cardNumberDisplay = document.getElementById("cardNumberDisplay");
	cardNumberDisplay.value = "1234 5678 9012 3456";
	document.getElementById("cardNumber").value = "1234567890123456";
	document.getElementById("card-preview").textContent = "1234 5678 9012 3456";
	
	document.getElementById("cardholder").value = "Lal Yapa";
	document.getElementById("holder-preview").textContent = "LAL YAPA";
	
	document.getElementById("expiry").value = "12/29";
	document.getElementById("expiry-preview").textContent = "12/29";
	
	document.getElementById("cvv").value = "123";
}

function demoDate(daysAhead) {
	const date = new Date();
	date.setDate(date.getDate() + daysAhead);

	return date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0") + "-" + String(date.getDate()).padStart(2, "0");
}

function fillMeetingForm(subject, daysAhead, startTime, endTime, location, notes) {
	document.getElementById("subject").value = subject;
	document.getElementById("date").value = demoDate(daysAhead);
	document.getElementById("startTime").value = startTime;
	document.getElementById("endTime").value = endTime;
	document.getElementById("location").value = location;
	document.getElementById("notes").value = notes;
}

function fillScheduleMeetingFormS1() {
	fillMeetingForm("Initial consultation meeting", 1, "11:00", "10:00", "LexFlow Office, Meeting Room 2", "Bring the supply agreement and the written complaints.");
}

function fillScheduleMeetingFormS2() {
	fillMeetingForm("Initial consultation meeting", 1, "10:00", "11:00", "LexFlow Office, Meeting Room 2", "Bring the supply agreement and the written complaints.");
}

function fillRescheduleMeetingFormS3() {
	fillMeetingForm("Initial consultation meeting", 2, "14:00", "15:00", "LexFlow Office, Meeting Room 1", "Moved at the client's request.");
}

function fillPartnerRejectionFormR() {
	document.getElementById("rejectionReason").value =
		"The conflict check found that the opposing party is an existing client of the firm, so the firm cannot act against them.";
}

function fillAssociateRejectionFormR() {
	document.getElementById("rejectionReason").value =
		"After the meeting, the matter was found to fall outside the firm's practice areas, so the firm cannot take it on.";
}

function fillContactForm(prefix, phoneNumber, email) {
	document.getElementById(prefix + "Name").value = "Sunil Wickramaratne";
	fillMultiInputRow(prefix + "PhoneRows", "phoneNumbers", phoneNumber);
	fillMultiInputRow(prefix + "EmailRows", "emails", email);
	fillMultiInputRow(prefix + "AddressRows", "addresses", "No. 47, Temple Road, Kadawatha");
	document.getElementById(prefix + "Notes").value = "Neighbour who built the boundary wall. Opposing party in this case.";
}

function fillContactFormP1(prefix) {
	fillContactForm(prefix, "07712", "sunil.wickramaratne@");
}

function fillContactFormP2(prefix) {
	fillContactForm(prefix, "0771234567", "sunil.wickramaratne@gmail.com");
}

function setTaskType(documenting, scheduling, billing) {
	const types = [["documentingTask", documenting], ["schedulingTask", scheduling], ["billingTask", billing]];

	for (let i = 0; i < types.length; i++) {
		const checkbox = document.querySelector("#taskAddModal input[name='" + types[i][0] + "']");
		checkbox.checked = types[i][1];
		checkbox.dispatchEvent(new Event("change", {bubbles: true}));
	}

}

function fillTaskForm(title, documenting, scheduling, billing) {
	document.getElementById("taskAddTitle").value = title;
	setTaskType(documenting, scheduling, billing);
	clearFileInput("taskAddDocumentFiles");
	document.getElementById("taskAddDate").value = "";
	document.getElementById("taskAddStartTime").value = "";
	document.getElementById("taskAddEndTime").value = "";
	document.getElementById("taskAddLocation").value = "";
	document.getElementById("taskAddNotes").value = "";
	document.getElementById("taskAddBillableAmount").value = "";
}

function fillTaskSchedule(daysAhead, startTime, endTime, location, notes) {
	document.getElementById("taskAddDate").value = demoDate(daysAhead);
	document.getElementById("taskAddStartTime").value = startTime;
	document.getElementById("taskAddEndTime").value = endTime;
	document.getElementById("taskAddLocation").value = location;
	document.getElementById("taskAddNotes").value = notes;
}

function fillTaskFormP1() {
	fillTaskForm("Drafting the letter of demand", false, false, true);
}

function fillTaskFormP2() {
	fillTaskForm("Drafting the letter of demand", false, false, true);
	document.getElementById("taskAddBillableAmount").value = "15000.00";
}

function fillTaskFormP3() {
	fillTaskForm("File the 2019 survey plan", true, false, false);
	fillFileInput("taskAddDocumentFiles", "/demo-files/case-document.pdf", "Survey Plan.pdf");
}

function fillTaskFormP4() {
	fillTaskForm("Site inspection at the boundary wall", false, true, false);
	fillTaskSchedule(3, "09:30", "10:30", "No. 45, Temple Road, Kadawatha", "Meet the licensed surveyor at the site.");
}

function fillTaskFormP5() {
	fillTaskForm("Court appearance at the District Court", false, true, true);
	fillTaskSchedule(5, "09:00", "12:00", "District Court, Gampaha", "First calling of the case.");
	document.getElementById("taskAddBillableAmount").value = "25000.00";
}

function fillConsultationFormR1() {
	document.getElementById("caseType").value = "CONTRACT_COMMERCIAL";
	fillMultiInputRow("opposingPartyRows", "opposingParties", "Meridian Textiles (Pvt) Ltd");
	document.getElementById("description").value = "   ";
}

function fillConsultationFormR2() {
	document.getElementById("caseType").value = "CONTRACT_COMMERCIAL";
	fillMultiInputRow("opposingPartyRows", "opposingParties", "Meridian Textiles (Pvt) Ltd");
	document.getElementById("description").value =
		"On 14 March 2026 our business entered into a twelve-month supply agreement with Meridian Textiles (Pvt) Ltd " +
		"for the delivery of finished cotton fabric on a fortnightly basis, at an agreed rate of LKR 850 per metre. " +
		"Since the fourth delivery cycle in late May, the quantities received have consistently fallen short of the " +
		"contracted volume by between 15% and 30%, and on two occasions the fabric supplied did not meet the agreed " +
		"GSM specification, forcing us to reject the batch and source replacement stock at short notice and at a " +
		"higher price from an alternative supplier. We have raised these shortfalls in writing on four separate " +
		"occasions, most recently on 2 September 2026, and have received only a verbal acknowledgement with no " +
		"corrective action or compensation offered. The repeated under-delivery has disrupted our own production " +
		"schedule and caused us to miss a delivery deadline to one of our largest retail clients, resulting in a " +
		"penalty clause being invoked against us. We are seeking advice on whether the pattern of shortfalls and " +
		"quality failures amounts to a material breach entitling us to terminate the agreement, recover the " +
		"additional procurement costs we have incurred, and pursue damages for the losses passed on to us by our " +
		"own client.";
}

function fillConsultationFormR3() {
	document.getElementById("caseType").value = "PROPERTY";
	fillMultiInputRow("opposingPartyRows", "opposingParties", "Sunil Wickramaratne");
	document.getElementById("description").value =
		"My neighbour Mr. Sunil Wickramaratne has built a boundary wall about two feet inside my land in Kadawatha. " +
		"The survey plan from 2019 shows the original boundary clearly. I would like advice on having the wall " +
		"removed and the boundary restored.";
}

function fillConsultationFormD() {
	document.getElementById("caseType").value = "EMPLOYMENT";
	fillMultiInputRow("opposingPartyRows", "opposingParties", "Brightline Logistics (Pvt) Ltd");
	document.getElementById("description").value =
		"I worked as a warehouse supervisor at Brightline Logistics (Pvt) Ltd for six years. On 1 August 2026 I was " +
		"dismissed with immediate effect after raising a written complaint about unpaid overtime for the previous " +
		"eight months. I received no warning letter, no disciplinary inquiry and no termination letter stating a " +
		"reason. My final salary has not been paid and my EPF and ETF contributions for the last three months have " +
		"not been remitted. I would like advice on challenging the termination before the Labour Tribunal, " +
		"recovering the unpaid overtime and salary, and compelling the company to settle the outstanding EPF and " +
		"ETF contributions. My appointment letter and the overtime complaint are attached.";
}

function fillConsultationFormD1() {
	fillConsultationFormD();
	fillLargeFileInput("consultationFiles", "Large Document.pdf");
}

function fillConsultationFormD2() {
	fillConsultationFormD();
	fillFileInput("consultationFiles", "/demo-files/consultation-document.pdf", "Consultation Document.pdf");
}

function fillCroConsultationFormR1() {
	fillCroConsultationFormR2();
	document.getElementById("description").value = "   ";
}

function fillCroConsultationFormR2() {
	// Lal Yapa — default seeded client, walked in to submit a new request.
	document.getElementById("clientCode").value = "CLT-31-70617268";
	document.getElementById("caseType").value = "PROPERTY";
	fillMultiInputRow("opposingPartyRows", "opposingParties", "Mr. S.B. Kulatunga (neighbour)");
	document.getElementById("description").value =
		"Mr. Yapa owns a residential property on Havelock Road, Colombo, inherited from his late father in 2019. " +
		"Earlier this year his neighbour, Mr. S.B. Kulatunga, began construction of a boundary wall that encroaches " +
		"approximately four feet onto Mr. Yapa's registered land, based on the original survey plan filed with the " +
		"land registry. When raised directly, Mr. Kulatunga produced a separate, more recent survey he commissioned " +
		"privately, which shows a different boundary line in his favour, and has refused to halt construction pending " +
		"an independent resurvey. The disputed strip includes an existing drainage channel that both properties rely " +
		"on, and continued construction risks blocking it entirely before the matter is resolved. Mr. Yapa would like " +
		"advice on obtaining an urgent injunction to halt further construction, the process for commissioning an " +
		"independent boundary resurvey admissible in court, and his options for recovering costs if the encroachment " +
		"is confirmed.";
}

function fillCroConsultationFormD() {
	// Lal Yapa — default seeded client, walked in with a second request and supporting documents.
	document.getElementById("clientCode").value = "CLT-31-70617268";
	document.getElementById("caseType").value = "CIVIL";
	fillMultiInputRow("opposingPartyRows", "opposingParties", "Ceylon Auto Traders");
	document.getElementById("description").value =
		"In June 2026 Mr. Yapa paid an advance of 1,200,000 to Ceylon Auto Traders for a reconditioned car that was to " +
		"be delivered within six weeks. The vehicle was never delivered, the showroom has since stopped answering " +
		"calls, and a written demand for a refund sent on 20 August 2026 has gone unanswered. Mr. Yapa would like " +
		"advice on recovering the advance through a civil action and on whether the matter should also be reported " +
		"to the police as cheating. The receipt for the advance and the demand letter are attached.";
}

function fillCroConsultationFormD1() {
	fillCroConsultationFormD();
	fillLargeFileInput("consultationFiles", "Large Document.pdf");
}

function fillCroConsultationFormD2() {
	fillCroConsultationFormD();
	fillFileInput("consultationFiles", "/demo-files/consultation-document.pdf", "Consultation Document.pdf");
}

function fillCashTopUpFormT1() {
	document.getElementById("clientCode").value = "CLT-00-00000000";
	document.getElementById("amount").value = -5000;
}

function fillCashTopUpFormT2() {
	// Lal Yapa — default seeded client.
	document.getElementById("clientCode").value = "CLT-31-70617268";
	document.getElementById("amount").value = 5000;
}

function fillWithdrawalFormT1() {
	document.getElementById("clientCode").value = "CLT-00-00000000";
	document.getElementById("amount").value = -1000;
}

function fillWithdrawalFormT2() {
	// Lal Yapa — default seeded client.
	document.getElementById("clientCode").value = "CLT-31-70617268";
	document.getElementById("amount").value = 1000;
}

function fillRegisterStaffFinance() {
	// Ashan Weerakone — a new finance officer, not yet in the system
	fillRegisterStaffRole("FINANCE_OFFICER", {
		email: "ashan.weerakone@lexflow.lk", name: "Ashan Weerakone", dateOfBirth: "1988-12-09",
		gender: "MALE", address: "No. 8, Marine Drive, Colombo", phoneNumber: "0761000005",
		nicNumber: "883435678V"
	});
}
