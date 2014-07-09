function dropdown2(ids, names, funs, after, i, selected, callback) {

	if (arguments.length < 5) {i = 0;};
	if (arguments.length < 6) {selected = "";};

	if (i == ids.length) {return;}
	
	function settoggle(i) {
		$("#"+ids[i]+"list").unbind('click');
		$("#"+ids[i]+"list").
			click(function (){$("#"+ids[i]).attr('value','').css('color','black').autocomplete('search').focus();});
	}
	
	function ac(i) {
		console.log(ids[i])
		$("#"+ids[i]).autocomplete({
			source: funs[i](i,selected),
			close: function(event,ui) {
				settoggle(ids[i]);
				if ($(this).attr('value') == "")
					$("#"+ids[i]).attr('value','-'+names[i]+'-').css('color','black');
			},
			blur: function(event) {event.preventDefault();},
			change: function(event,ui) {
				console.log("dropdown2: Change event triggered on " + i);				
				

				if (ui.item == null) {
					ui.item = {};
					ui.item.value = $(this).attr("value");
					return;
				}

				$(after+i).attr('name',ids[i]).attr('value',ui.item.value);
				console.log(ui.item.value)
				console.log(ui)
				// This is to deal with the fact that change() is called on a blur event.
				if (ui.item.value !== ui.item.valuelast) {
					console.log("           and value has changed.  Removing all following drop-downs.");
					for (j = i+1; j < ids.length; j++) {
						$(after + (j)).html('');
					}
				} else {
					console.log("           and value has not changed.  Not removing " + after+(i+1));
				}
			},			
			select: function(event,ui) {
						console.log("dropdown2: Select event triggered on " + i + " with value " + ui.item.value);

						// Programatically select item with
						// $("#catalog").val("cdaweb").data("autocomplete")._trigger("select",event,{item:"cdaweb"})
						// In this case, use event.targe.value as ui.item is undefined.
						
						val = ui.item.value || event.target.value;
						console.log("Val: " + val)
						$(after+i).attr('name',ids[i]).attr('value',val);
						console.log(location.hash === "")
						if (location.hash === "") {
							var qs = {};
						} else {
							var qs = $.parseQueryString();
						}
							
							
							console.log(qs)
							//if ($("#dropdowns"+i).val().length > 0) {
							qs[$("#dropdowns" + (i)).attr('name')] = $("#dropdowns"+i).val();
							//}
							for (j = i+1; j < ids.length; j++) {
								console.log("Deleting "+$("#dropdowns" + (j)).attr('name'))
								delete qs[$("#dropdowns" + (j)).attr('name')];
							}
						for (j = i+1; j < ids.length; j++) {
							$("#dropdowns" + (j)).html('');
							console.log("Deleting "+$("#dropdowns" + (j)).attr('name'))
						}
						console.log("-----")
						console.log(qs)
						console.log("xSetting hashchange.byurledit to false");
						$(window).hashchange.byurledit = false;
						location.hash = decodeURIComponent($.param(qs));

						funs[i].onselect();
						dropdown2(ids, names, funs, after, i+1, val, callback);
						$(after+(i+1)).show();
						if (ui.item)
							ui.item.valuelast = ui.item.value;
			},
			minLength: 0
		}).click(function () {
			$(this).attr('value','').css('color','black').autocomplete('search');
		});
	}

	console.log("dropdown2: Creating dropdown " + i);

	$(after+(i))
		.append('<span class="ui-widget" style="display:table;width:100%;border:1px solid black;"></span>');
	$(after + (i) + " .ui-widget")
		.append('<span class="dropdown2" style="width:2px;display:table-cell"></span>')
		.append('<input id="'+ids[i]+'" class="dropdown2"  title="Enter text to search list" style="width:100%;display:table-cell;color:black;font-weight:bold;text-align:center;" value="-'+names[i]+'-"/>')
		.append('<span class="dropdown2" style="width:5px;display:table-cell"></span>')
		.append('<label id="'+ids[i]+'list" class="dropdown2" title="Show full list" style="width:1em;display:table-cell;cursor:pointer">&#9660;</label>');
	
	// Append is syncronous, so this won't happen before element is in DOM.
	// Allow entries not in list.
	$(after + (i) + ' input[id=' + ids[i] + ']').live('input',function () {
		$(this).parent().parent().attr('value',$(this).attr('value'));
	});
	

	ac(i);
	settoggle(i);
	if (callback)
		callback(ids[i],i);

}