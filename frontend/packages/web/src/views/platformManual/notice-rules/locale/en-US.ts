export default {
  'platformManualNoticeRules.intro':
    'Every platform notification is managed centrally in "Message Settings", where you can toggle it, change the channel and edit the copy. The table below lists all platform notifications with their trigger and recipient; the actual toggle and channel follow what is configured in "Message Settings".',
  'platformManualNoticeRules.eventsTitle': '9 Platform Notifications',
  'platformManualNoticeRules.col.event': 'Notification',
  'platformManualNoticeRules.col.trigger': 'When it fires',
  'platformManualNoticeRules.col.receiver': 'Recipient',

  'platformManualNoticeRules.event.registerApply.name': 'Enterprise signup submitted',
  'platformManualNoticeRules.event.registerApply.trigger': 'After an enterprise submits signup',
  'platformManualNoticeRules.event.registerApply.receiver': 'Platform admin',
  'platformManualNoticeRules.event.registerApproved.name': 'Enterprise signup approved',
  'platformManualNoticeRules.event.registerApproved.trigger': 'After audit approval',
  'platformManualNoticeRules.event.registerApproved.receiver': 'Applicant',
  'platformManualNoticeRules.event.registerRejected.name': 'Enterprise signup rejected',
  'platformManualNoticeRules.event.registerRejected.trigger': 'After audit rejection',
  'platformManualNoticeRules.event.registerRejected.receiver': 'Applicant',
  'platformManualNoticeRules.event.planExpire.name': 'Plan expiry reminder',
  'platformManualNoticeRules.event.planExpire.trigger': 'When the plan is about to expire',
  'platformManualNoticeRules.event.planExpire.receiver': 'Tenant admin',
  'platformManualNoticeRules.event.planApply.name': 'Renewal / upgrade request',
  'platformManualNoticeRules.event.planApply.trigger': 'After a tenant submits a renewal request',
  'platformManualNoticeRules.event.planApply.receiver': 'Platform admin',
  'platformManualNoticeRules.event.planApproved.name': 'Renewal approved',
  'platformManualNoticeRules.event.planApproved.trigger': 'After renewal write-off is approved',
  'platformManualNoticeRules.event.planApproved.receiver': 'Tenant admin',
  'platformManualNoticeRules.event.planRejected.name': 'Renewal rejected',
  'platformManualNoticeRules.event.planRejected.trigger': 'After renewal is rejected',
  'platformManualNoticeRules.event.planRejected.receiver': 'Tenant admin',
  'platformManualNoticeRules.event.quotaBreak.name': 'AI quota exhausted',
  'platformManualNoticeRules.event.quotaBreak.trigger': 'When the tenant runs out of AI quota',
  'platformManualNoticeRules.event.quotaBreak.receiver': 'Tenant admin',
  'platformManualNoticeRules.event.paymentVerified.name': 'Payment verified',
  'platformManualNoticeRules.event.paymentVerified.trigger': 'After payment write-off is done',
  'platformManualNoticeRules.event.paymentVerified.receiver': 'Tenant admin',

  'platformManualNoticeRules.specialTitle': 'Notes',
  'platformManualNoticeRules.special.paymentNoSms':
    'Payment verification sends only an in-app notice, no SMS: the tenant\'s "Personal Center → Subscription" page refreshes automatically after verification.',
  'platformManualNoticeRules.special.renewalNoDup':
    'A renewal write-off goes through the "Renewal approved" notification and will not additionally send "Payment verified", so the tenant never receives two notices.',
  'platformManualNoticeRules.special.personalNoAudit':
    'Personal signup is exempt from audit and sends no notification; a free trial is provisioned immediately on submit.',
  'platformManualNoticeRules.special.registerApplyWecomGroup':
    'Enterprise signup now pushes to a WeCom group: once you fill in the "Group bot Webhook" under "Org settings → Third-party integration / WeCom", each enterprise signup pushes a message to the admin group (company name, contact, phone, apply time, approval link); personal signup does not. The group-bot mode needs no corp ID / app ID / secret and no trusted-IP setup.',
  'platformManualNoticeRules.special.wecomLoginClosed':
    'WeCom QR login is closed: the login page keeps only password and SMS code. The approval notification now includes a "WeCom one-tap login not yet supported" note to guide users to log in with a password.',
};
