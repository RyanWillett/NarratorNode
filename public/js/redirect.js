(function(){
	if(!localStorage)
		return;
	var width = $(window).width();
	var mobileSite = window.location.href.indexOf("mobile") !== -1;
	var hasPreference = localStorage.getItem("prefMobile") !== null;
	var mobilePreference = localStorage.getItem("prefMobile");
	var smallSize = width < 600;

	if(mobileSite){
		if(!hasPreference){
			if(!smallSize)
				redirect(!mobileSite);
		}else{
			if(!mobilePreference)
				redirect(!mobileSite);
		}
	}else{
		if(!hasPreference){
			if(smallSize)
				redirect(mobileSite);
		}else{
			if(mobilePreference)
				redirect(mobileSite);
		}
	}

	function redirect(toMobile){
		if(toMobile)
			window.location = window.location.href.replace("mobile.html", "");
		else
			window.location = window.location.href + "mobile.html";
	}
})();

