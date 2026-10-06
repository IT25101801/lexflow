// demo.js – pre-fills forms with sample data for demo purposes.

function fillLogin(email, password) {
	document.getElementById("username").value = email;
	document.getElementById("password").value = password;
}

// Delta's addresses/phoneNumbers are multi-input-row lists, not single fields -- fill
// just the first row, which every registration form starts with exactly one of.
function fillMultiInputRow(containerId, fieldName, value) {
	const input = document.querySelector("#" + containerId + " input[name='" + fieldName + "']");
	if (input) input.value = value;
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
async function fillFileInput(inputId, url, filename) {
	const input = document.getElementById(inputId);
	if (!input) return;
	const response = await fetch(url);
	const blob = await response.blob();
	const file = new File([blob], filename, {type: "application/pdf"});
	const dataTransfer = new DataTransfer();
	dataTransfer.items.add(file);
	input.files = dataTransfer.files;
	input.dispatchEvent(new Event("change", {bubbles: true}));
}

function fillClientRegisterFormB() {
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
	fillFileInput("nicFile", "/demo-files/nic-document.pdf", "NIC Document.pdf");
}

function fillWalkinClientRegisterFormB() {
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

function fillPaymentForm() {
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

function fillConsultationFormR() {
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
	fillFileInput("consultationFiles", "/demo-files/consultation-document.pdf", "Consultation Document.pdf");
}

function fillCroConsultationFormR() {
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
	fillFileInput("consultationFiles", "/demo-files/consultation-document.pdf", "Consultation Document.pdf");
}

function fillCashTopUpForm() {
	// Lal Yapa — default seeded client.
	document.getElementById("clientCode").value = "CLT-31-70617268";
	document.getElementById("amount").value = 5000;
}

function fillWithdrawalForm() {
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
