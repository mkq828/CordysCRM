export default {
  // Register audit
  'platformManualUsage.registerAudit.intro':
    'The review gate for enterprise sign-ups: approving opens a tenant automatically, rejecting notifies the applicant.',
  'platformManualUsage.registerAudit.screenshotCaption': 'Register audit list',
  'platformManualUsage.registerAudit.what':
    'Enterprise sign-up requests queue here for review. You verify their qualifications and decide to approve or reject. Personal sign-ups skip review and open automatically, so they never appear here.',
  'platformManualUsage.registerAudit.where': 'Left menu “Tenant lifecycle” → “Register audit”',
  'platformManualUsage.registerAudit.step1': 'Open the page; the list shows pending requests by default.',
  'platformManualUsage.registerAudit.step2': 'Search by name, company name, or phone number.',
  'platformManualUsage.registerAudit.step3': 'Click the request’s review button and check the company details.',
  'platformManualUsage.registerAudit.step4':
    'If it looks good, click Approve — the system opens the tenant automatically (creates the account and starts a free trial).',
  'platformManualUsage.registerAudit.step5':
    'If not, click Reject and give a reason; the applicant gets an SMS notification.',
  'platformManualUsage.registerAudit.example':
    'A new customer “XX Tech” submits an enterprise sign-up. You find it in the list, verify the details, and click Approve — it opens a free trial and shows up in the “Paid users” list.',
  'platformManualUsage.registerAudit.tip':
    'Approve opens immediately, so don’t click it by mistake. Read the applicant’s details before rejecting.',

  // Paid users
  'platformManualUsage.paidUser.intro':
    'The master list of all open tenants: version, expiry, signing/follow-up manager.',
  'platformManualUsage.paidUser.screenshotCaption': 'Paid users list',
  'platformManualUsage.paidUser.what':
    'Here you see each tenant’s current version, when it expires, and which signing/follow-up manager owns it. Manage the tenant lifecycle and check who is about to expire from here.',
  'platformManualUsage.paidUser.where': 'Left menu “Tenant lifecycle” → “Paid users”',
  'platformManualUsage.paidUser.step1': 'Search by org name, phone number, signing manager, or follow-up manager.',
  'platformManualUsage.paidUser.step2': 'Read the Version, Expiry, and Days left columns.',
  'platformManualUsage.paidUser.step3': 'To adjust a trial, click Trial settings in the top-right.',
  'platformManualUsage.paidUser.step4': 'Click a tenant row to open its details and view or edit its plan.',
  'platformManualUsage.paidUser.example':
    'To see how many days “Hou Yue” has left, search the name and read the Days left column.',
  'platformManualUsage.paidUser.tip':
    'After the first payment is reconciled, the status here changes automatically from Free trial to Enterprise.',

  // Renewal / upgrade applications
  'platformManualUsage.planApplication.intro': 'Tenant renewal/upgrade requests; reconcile after you confirm payment.',
  'platformManualUsage.planApplication.screenshotCaption': 'Renewal application list',
  'platformManualUsage.planApplication.what':
    'When a tenant is about to expire, it submits a renewal or upgrade request that queues here. You confirm the payment has arrived and click Approve, and its validity is extended.',
  'platformManualUsage.planApplication.where': 'Left menu “Tenant lifecycle” → “Renewal application”',
  'platformManualUsage.planApplication.step1': 'Open the page and look at the pending requests.',
  'platformManualUsage.planApplication.step2':
    'Open a request to see the version change, amount, validity, and payment proof.',
  'platformManualUsage.planApplication.step3': 'Once you confirm the money has arrived, click Approve to reconcile.',
  'platformManualUsage.planApplication.step4':
    'If it hasn’t arrived or the info is wrong, click Reject and give a reason.',
  'platformManualUsage.planApplication.example':
    'Customer “Li Jun” renews his Basic plan. You confirm the payment proof, click Approve, and his expiry is extended by 365 days.',
  'platformManualUsage.planApplication.tip':
    'Only a renewal reconcile extends validity; rejecting leaves the plan untouched.',

  // Contracts
  'platformManualUsage.contract.intro': 'Manage each tenant’s contracts, from draft to archive.',
  'platformManualUsage.contract.screenshotCaption': 'Contract list',
  'platformManualUsage.contract.what':
    'Every signed contract is recorded here, moving through Draft → Pending signature → Completed → Archived. The contract amount is the basis for payments and invoicing.',
  'platformManualUsage.contract.where': 'Left menu “Tenant lifecycle” → “Contract”',
  'platformManualUsage.contract.step1':
    'Click New contract and fill in the tenant, plan version, amount, and subscription length.',
  'platformManualUsage.contract.step2':
    'Upload the contract scan (first version is signed offline — a photo or scan is fine).',
  'platformManualUsage.contract.step3': 'Update the status as it progresses, marking it Completed once signed.',
  'platformManualUsage.contract.example':
    'A customer signs a ¥1680 yearly contract. You create it, fill the amount, upload the scan, mark it Completed — later payments can be linked to this contract.',
  'platformManualUsage.contract.tip': 'The payment amount usually equals the contract amount, so don’t get it wrong.',

  // Payments
  'platformManualUsage.paymentRecord.intro':
    'Record each payment and reconcile it; only after reconciliation does a tenant take effect or renew.',
  'platformManualUsage.paymentRecord.screenshotCaption': 'Payment list',
  'platformManualUsage.paymentRecord.what':
    'After a customer pays, you record a payment here, upload the proof, and click Reconcile to confirm arrival. The first reconcile automatically switches the tenant to Enterprise.',
  'platformManualUsage.paymentRecord.where': 'Left menu “Tenant lifecycle” → “Payment”',
  'platformManualUsage.paymentRecord.step1':
    'Click Payment to add one; choose the linked contract, amount, and payment method.',
  'platformManualUsage.paymentRecord.step2': 'Upload the payment proof; the status is now Pending reconcile.',
  'platformManualUsage.paymentRecord.step3':
    'Once the money has arrived, click Reconcile — the status becomes Completed.',
  'platformManualUsage.paymentRecord.example':
    'Hou Yue’s first ¥3980 payment arrives. You add and reconcile it, and the system switches her plan to Enterprise automatically.',
  'platformManualUsage.paymentRecord.tip':
    'Undoing a reconcile only reverses the bookkeeping; it doesn’t close the Enterprise plan.',

  // Invoices
  'platformManualUsage.invoice.intro': 'Invoice reconciled payments; record the invoice type and status.',
  'platformManualUsage.invoice.screenshotCaption': 'Invoice list',
  'platformManualUsage.invoice.what':
    'When a customer asks for an invoice, you register the details here and update the status once issued.',
  'platformManualUsage.invoice.where': 'Left menu “Tenant lifecycle” → “Invoice”',
  'platformManualUsage.invoice.step1': 'Click New invoice and link the contract.',
  'platformManualUsage.invoice.step2': 'Pick the invoice type (regular/VAT) and fill in the amount.',
  'platformManualUsage.invoice.step3': 'Once issued, change the status to Issued.',
  'platformManualUsage.invoice.example':
    'Hou Yue paid ¥3980 and wants a regular invoice. You create it, fill the amount, and mark it complete once issued.',
  'platformManualUsage.invoice.tip': 'The invoice amount usually equals the contract amount.',

  // Revenue dashboard
  'platformManualUsage.revenue.intro': 'See contract amount, collected, outstanding, and invoiced at a glance.',
  'platformManualUsage.revenue.screenshotCaption': 'Revenue dashboard',
  'platformManualUsage.revenue.what':
    'This aggregates the platform’s overall revenue: contracts signed, collected, outstanding, and invoiced.',
  'platformManualUsage.revenue.where': 'Left menu “Tenant lifecycle” → “Revenue”',
  'platformManualUsage.revenue.step1': 'Pick a dimension: weekly / monthly / yearly.',
  'platformManualUsage.revenue.step2': 'Click Refresh to see the latest numbers.',
  'platformManualUsage.revenue.step3': 'Read the four cards: Contract amount, Collected, Outstanding, and Invoiced.',
  'platformManualUsage.revenue.example':
    'To report this month’s revenue, switch to Monthly and read the Collected amount.',

  // Partner accounts
  'platformManualUsage.cityAccount.intro':
    'Manage city-partner accounts; see how many customers each signs up or follows.',
  'platformManualUsage.cityAccount.screenshotCaption': 'Partner accounts',
  'platformManualUsage.cityAccount.what':
    'City partners (city managers) are the people who bring you customers. Create accounts, assign tenants to them, and see how many customers each one owns.',
  'platformManualUsage.cityAccount.where': 'Left menu “City partners” → “Partner accounts”',
  'platformManualUsage.cityAccount.step1': 'Click Add manager to create a partner account.',
  'platformManualUsage.cityAccount.step2': 'Click Assign to attach tenants to a manager.',
  'platformManualUsage.cityAccount.step3': 'Read each person’s Signed customers and Followed customers counts.',
  'platformManualUsage.cityAccount.example':
    'A new city manager joins. You create their account, then assign a few tenants as their follow-up manager.',
  'platformManualUsage.cityAccount.tip':
    'When a manager leaves, disable the account and transfer their customers to someone else.',

  // Performance dashboard
  'platformManualUsage.cityDashboard.intro': 'See signed customers, contract amount, and payments per city partner.',
  'platformManualUsage.cityDashboard.screenshotCaption': 'Performance dashboard',
  'platformManualUsage.cityDashboard.what':
    'Summarize performance per person: customers signed, contract amount, and payments collected.',
  'platformManualUsage.cityDashboard.where': 'Left menu “City partners” → “Performance”',
  'platformManualUsage.cityDashboard.step1': 'Pick a dimension (weekly/monthly/yearly).',
  'platformManualUsage.cityDashboard.step2': 'Click Refresh.',
  'platformManualUsage.cityDashboard.step3':
    'Read each manager’s Signed customers, Contract amount, and Payment amount.',
  'platformManualUsage.cityDashboard.example':
    'To see how much “Deng Peng” signed this month, read his Signed customers and Contract amount columns.',

  // Editions
  'platformManualUsage.edition.intro': 'Configure sellable versions (Basic/Enterprise, etc.) with prices and quotas.',
  'platformManualUsage.edition.screenshotCaption': 'Editions',
  'platformManualUsage.edition.what':
    'Here you define which versions you sell, how much each costs, how many seats, and how much AI quota it grants. Tenants follow this config when they open.',
  'platformManualUsage.edition.where': 'Left menu “Platform ops” → “Editions”',
  'platformManualUsage.edition.step1': 'Click New edition and fill in the edition code and name.',
  'platformManualUsage.edition.step2':
    'Fill in yearly price, first-year price, seat cap, validity days, and monthly AI quota.',
  'platformManualUsage.edition.step3': 'Save and set the status to Enabled.',
  'platformManualUsage.edition.example':
    'To add a “Flagship” edition, create it with price and quota, then enable it so tenants can choose it.',
  'platformManualUsage.edition.tip':
    'Changing price or quota only affects newly opened tenants; existing snapshots stay unchanged.',

  // System logs
  'platformManualUsage.log.intro': 'See who did what and when, including login and AI-execution logs.',
  'platformManualUsage.log.screenshotCaption': 'System logs',
  'platformManualUsage.log.what':
    'Every key operation is recorded here: who did it, what changed, and on which page. When something goes wrong, come here to find out who did it.',
  'platformManualUsage.log.where': 'Left menu “Platform ops” → “System logs”',
  'platformManualUsage.log.step1': 'Switch between Operation / Login / AI-execution logs.',
  'platformManualUsage.log.step2': 'Filter by operator, time, operation type, and target.',
  'platformManualUsage.log.step3': 'Open a row to see the details.',
  'platformManualUsage.log.example':
    'Someone wrongly edited a customer record. In Operation logs, filter by operator to find who changed it and when.',
  'platformManualUsage.log.tip':
    'Server errors don\'t land here — look them up by traceId in "System error logs" next door; check whether scheduled jobs ran and why they failed in "Scheduled job execution logs".',

  // Bug reports
  'platformManualUsage.bugReport.intro': 'User-submitted issue reports with page and traceId for easy tracing.',
  'platformManualUsage.bugReport.screenshotCaption': 'Bug reports',
  'platformManualUsage.bugReport.what':
    'When users hit a problem, they can submit it from the Feedback entry, which includes the page route and traceId. You triage and follow up here.',
  'platformManualUsage.bugReport.where': 'Left menu “Platform ops” → “Bug reports”',
  'platformManualUsage.bugReport.step1': 'Read the list and search by description or submitter.',
  'platformManualUsage.bugReport.step2': 'Open a report to see the description, page route, and traceId.',
  'platformManualUsage.bugReport.step3': 'Look up the traceId in System logs, fix the issue, then update the status.',
  'platformManualUsage.bugReport.example':
    'A user reports an error on a page. You find the exception in logs by traceId, fix it, and mark the report as handled.',

  // AI quota
  'platformManualUsage.aiQuota.label': 'Quota packs & billing rules',
  'platformManualUsage.aiQuota.intro':
    'Platform-side AI billing and quota: watch cost, set unit prices, grant quota packs.',
  'platformManualUsage.aiQuota.screenshotCaption': 'AI quota management',
  'platformManualUsage.aiQuota.what':
    'This is where the platform manages AI spend and quotas: how much it costs, how each model is priced, and how much quota each tenant gets.',
  'platformManualUsage.aiQuota.where': 'Left menu “AI quota management”',
  'platformManualUsage.aiQuota.step1': 'Cost dashboard shows total cost, tokens, and call counts.',
  'platformManualUsage.aiQuota.step2': 'Model pricing sets the price per model — this is the billing rule.',
  'platformManualUsage.aiQuota.step3': 'Global config sets defaults like the default quota.',
  'platformManualUsage.aiQuota.step4':
    'Quota packs creates packs, Tenant quota assigns them, and Quota pack orders shows purchase history.',
  'platformManualUsage.aiQuota.example':
    'A newly signed Enterprise tenant should get 1000 AI calls. You create a pack under Quota packs and assign it under Tenant quota.',
  'platformManualUsage.aiQuota.tip': 'The platform admin account doesn’t consume quota when using AI itself.',

  // Security events
  'platformManualUsage.securityEvent.intro':
    'Records malicious requests and attacks the system detects, exportable as evidence.',
  'platformManualUsage.securityEvent.screenshotCaption': 'Security events list',
  'platformManualUsage.securityEvent.what':
    'The system detects SQL injection, XSS, path traversal, scanner probing and error flooding in real time — it only records, never blocks. Here you see who attacked the system and when, and export CSV as security evidence when needed.',
  'platformManualUsage.securityEvent.where': 'Left menu “Platform ops” → “Security events”',
  'platformManualUsage.securityEvent.step1': 'Open the page and filter by event type, IP, or time.',
  'platformManualUsage.securityEvent.step2':
    'Click View on a row to see IP, User-Agent, request URI, forward chain, region, traceId, etc.',
  'platformManualUsage.securityEvent.step3':
    'To keep evidence, click Export CSV to export the current filtered results.',
  'platformManualUsage.securityEvent.example':
    'An IP keeps probing your login endpoint. You filter by that IP here, see a run of “Scanner” events, and export the CSV as evidence.',
  'platformManualUsage.securityEvent.tip':
    'Security events are recorded, not auto-blocked; if there is a real attack, block the IP on your firewall or WAF.',

  // System error logs
  'platformManualUsage.errorLog.intro': 'See server errors in one place, locate by traceId, with the full stack trace.',
  'platformManualUsage.errorLog.screenshotCaption': 'System error logs',
  'platformManualUsage.errorLog.what':
    'Every server error is recorded with the error class, message, stack trace, traceId and time. When a user reports a problem or a page returns 500, come here to pinpoint it by traceId.',
  'platformManualUsage.errorLog.where': 'Left menu “Platform ops” → “System error logs”',
  'platformManualUsage.errorLog.step1': 'Filter by error class, traceId, or a keyword in the message.',
  'platformManualUsage.errorLog.step2': 'Click View stack to see the full trace and locate which code or SQL failed.',
  'platformManualUsage.errorLog.step3': 'Match the traceId to a Bug report or user report, fix it, then follow up.',
  'platformManualUsage.errorLog.example':
    'A user reports a 500 on a page. You take the traceId from the report, search it here, read the stack to find a null pointer, fix it, and confirm it no longer occurs.',
  'platformManualUsage.errorLog.tip':
    'Error records keep piling up, so clean them periodically; prefer traceId when troubleshooting — it is the fastest way to locate.',

  // Scheduled job execution logs
  'platformManualUsage.jobExecLog.intro': 'See whether each scheduled job ran, how long it took, and why it failed.',
  'platformManualUsage.jobExecLog.screenshotCaption': 'Scheduled job execution logs',
  'platformManualUsage.jobExecLog.what':
    'Every scheduled job (e.g. AI-employee follow-up, morning report push, data rollup) records a row per run: job name, status (running/success/failed), start/end time, and duration. Failures also record the failure message.',
  'platformManualUsage.jobExecLog.where': 'Left menu “Platform ops” → “Scheduled job execution logs”',
  'platformManualUsage.jobExecLog.step1': 'Filter by job name, status, or time range.',
  'platformManualUsage.jobExecLog.step2':
    'Read the status and duration columns to spot jobs that failed or ran very slowly.',
  'platformManualUsage.jobExecLog.step3': 'Open a failed record to read the failure message, then fix the cause.',
  'platformManualUsage.jobExecLog.example':
    'The AI employee didn’t auto-follow-up today. You filter by the job name, find status “Failed”, open it and see the failure was an API timeout.',
  'platformManualUsage.jobExecLog.tip':
    'Scheduled jobs have been moved to run off-peak around midnight; if one fails overnight, check here during the day.',
};
