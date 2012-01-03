var map;
var first = true;
  function initialize() {
    // Retrieve stations.

    var latlng = new google.maps.LatLng(32.066792, 34.777694);  // Merkaz Tel Aviv
    var myOptions = {
      zoom: 17,
      center: latlng,
      mapTypeControl: false,
      scaleControl: true,
      streetViewControl: false,
      zoomControl: true,
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map = new google.maps.Map(document.getElementById("map_canvas"),
        myOptions);
    google.maps.event.addListener(map, 'click', function() {
        if (openInfoWindow) {
          openInfoWindow.close();
          openInfoWindow = null;
        }
		});
  }

function calcTriang() {
  var lat = parseFloat($('#latlng').val().split(',')[0]);
  var lng = parseFloat($('#latlng').val().split(',')[1]);
  var dist = parseFloat($('#dist').val());
  new google.maps.Circle({map: map, center: new google.maps.LatLng(lat, lng), radius: dist});
  if (first) {
    first = false;
    map.setCenter(new google.maps.LatLng(lat, lng));
  }
}

function populateMyLocation() {
  navigator.geolocation.getCurrentPosition(function(position) { 
    $('#latlng').val(position.coords.latitude + ', ' + position.coords.longitude);
    $("#dist").val(position.coords.accuracy);
  }, null, {maximumAge: 5000});
}

