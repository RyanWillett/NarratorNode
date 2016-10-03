$(document).ready(function(){
	$("#tutorialButton").click(function(){
		window.location = 'tutorial.html';
	});
	$("#logoutButtonMobile").click(function(){
		firebase.auth().signOut();
	});
});


