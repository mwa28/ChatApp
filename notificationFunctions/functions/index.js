const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

exports.sendNotification = functions.database.ref('/notification/{user_id}/{notification_id}').onWrite((data, context) => { 
	const user_id = context.params.user_id; 
	const notification_id = context.params.notification_id;
	console.log('We have a notification to send to : ', user_id);
	
	if(event.data.val()){
		return console.log('A notification has been deleted from the database: ', notification_id);
	}
	
	const from_user =  admin.database().ref(`/notification/${user_id}/${notification_id}`).once('value');
	
	return from_user.then(fromUserResult =>
	{
		const from_user_id = fromUserResult.val().from;
		console.log('You have new notification from :', from_user);
		
		const userQuery = admin.database().ref(`/Users/${from_user_id}/name}`).once('value');
		const deviceToken = admin.database().ref(`/Users/${user_id}/device_token`).once('value');
		
		return Promise.all(userQuery, deviceToken).then(result =>{
			const userName = result[0];
			const token_id = result[1];
			const payload = {
				notification: {
					title : "Friend Request",
					body : `${userName} has sent you a friend request`,
					icon: "default",
					click_action: "moh.chatapp_TARGET_NOTIFICATION"
				},
				data : {
				from_user_id: from_user_id
				}
			};
			return admin.messaging().sendToDevice(token_id, payload).then(response => { return console.log('This was the notification Feature'); }); 
		});
	});
});