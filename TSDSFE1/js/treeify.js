function treeify(names,level,parent) {

	delim = ".";
	delimre = /\./;
	debug = true;
	
	names.sort();
	if (!treeify.n) {
		treeify.n = 0;
	} else {
		treeify.n = treeify.n+1;
	}
	if (!parent) {
		console.log("setting parent to blank")
		var parent = "";
	}
	if (debug) {
		console.log("Level = " + level);
		console.log("names = ")
		console.log(names);
	}
	console.log("parent: " + parent)
	
	var cont = 0;

	var L  = new Array();
	var Lr = new Array();

	var a = 0;
	var N = 0;
	var k = 0;
	var D = new Object();

	if (names.length == 1) { 
		if (debug) {
			console.log("names has one element");
			console.log(names[0]);
		}
		if (names[0].match(delimre)) {
			if (debug)
				console.log("names has one element with delimeter");
			tmpa  = names[0].split(delim);
			L    = tmpa[0];
			//D[L] = new Object();
			D[parent+L] = tmpa[1];
		} else {
			if (debug)	
				console.log("names has one element without delimeter")
			return names;
			//D[names[0]] = names[0];
		}
		return D;
	}

	done = true;
	for (var i = 0;i < names.length;i++) {
		if (names[i].match(delim)) {
			done = false;
			continue;
		}
	}
	if (done) {
		if (debug)
			console.log("no delimiters")
		//console.log(names);
		return ".";
	}
	
	var ilast=0;
	for (var i = 0;i < names.length;i++) {
		var tmpa = [];

		tmpa  = names[i].split(".");
		L[i]  = tmpa[0];
		Lr[i] = tmpa.slice(1).join(".");

		if (Lr[i] != "") {
			cont = 1;
		}
		console.log("parent " + i + parent)
		if (debug) {
			if (i > 0) {
				console.log("Current prefix: "+L[i]+"; Last prefix: "+L[i-1]);
			} else {
				console.log("Current prefix: "+L[i]+"; Last prefix: undefined");
			}
			console.log("parent: " + parent)
		}
		
		if (debug) {
			console.log("Current remainder")
			console.log(Lr)
		}
		
		if (cont == 0) {
			console.log('cont=0');
			console.log('parent='+parent)
			continue;
		}
		if (i == names.length-1 && L[i] == L[i-1]) {
			if (debug) {
				console.log("-Creating new directory named " + L[i-1]);
				console.log("-Call treeify.");
				console.log('-ilast: '+ilast)
			}
			var lt = Lr.slice(ilast);
			D[L[i-1]] = treeify(lt,level+1,L[i-1]);			
		} else if (i > 0 && L[i] != L[i-1]) {
			if (debug) {
				console.log("+Creating new directory named " + L[i-1]);
				console.log("+Call treeify.");
				console.log('+ilast: '+ilast)
			}
			var lt = Lr.slice(ilast,i);
			console.log("a parent: " + parent);
			tmp = treeify(lt,level+1,L[i-1]);
			console.log("b parent: " + parent);
			if (tmp == ".") {
				console.log(i);
				console.log(ilast);
				console.log(names.slice(ilast,i));
				D["."] = names.slice(ilast,i);
			} else {
				console.log("xCall treeify with remainder");
				console.log("parent: " + parent);
				D[L[i-1]] = treeify(lt,level+1,L[i-1]);
			}
			if (i == names.length - 1 ) {
				if (debug) {
					console.log("Creating new directory named " + L[i]);
					console.log("Call treeify with remainder");
				}
				D[L[i]] = treeify(Lr.slice(i),level+1,L[i]);
			}
			ilast = i;
		}  

	}
	
	if (Object.keys(D).length === 0) {
		return names
	}
	
	return D;
}

