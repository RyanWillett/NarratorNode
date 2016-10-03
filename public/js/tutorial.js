var imageNumber = 1;

function setImage(){
	if(imageNumber === 7)
		parent.history.back();
	var image_name = 'step' + imageNumber + ".png";
	document.getElementById("mainBody").style.cssText = "background: url('../" + image_name + "') no-repeat center center fixed;-webkit-background-size: cover;-moz-background-size: cover;-o-background-size: cover;background-size: cover;";
	imageNumber++;
	console.log(imageNumber);
}

setImage();
document.getElementById("mainBody").onclick=setImage;