// Constants.
  var START_ROW = 4;
  var ROWS_PER_SAMPLE = 5;
  var RATIO_PROBLEM = 0.15;  // ratio above which there is a problem with the station
  var ONE_OR_TWO_COMPARED_TO_NONE = 0.25;  // how to weigh 1-2 bikes/docks compared to none
  var neBound = new google.maps.LatLng(32.13194, 34.847946);
  var swBound = new google.maps.LatLng(32.030381, 34.739285);
  var tlvBounds = new google.maps.LatLngBounds(swBound, neBound);
  var oldSize = 0;
  var placeService;
  var geocoder;

// Global vars.
  var stations = {};  // static information about stations, mapped by id.
  var stationsInfo = {};  // dynamic information about stations, mapped by id.
  var openInfoWindow = null;
  var map = {};
  var userPosition = null;
  var myPosCircle = undefined;
  var timeUpdateTimeout = undefined;
  var lastUpdateTime = undefined;
  var directionsRenderer = 
      new google.maps.DirectionsRenderer(
          {suppressMarkers: true});
  var namesLoaded = false;
  var locationsLoaded = false;
  var bikeStatusLoaded = true;
  var scoresLoaded = false;
  var distanceSortedStations = [];
  var scoreSortedStations = [];


  function numericTimeToHumanTime(time) {
    var hour = String(Math.floor(time));
    var mins = String(60 * (time - Math.floor(time)));
    if (mins == '0')
      mins = '00';
    return hour + ':' + mins;
  }
  function timeChanged(time) {
    $('#timeLabel').html(numericTimeToHumanTime(time));
  }

  function getFloatStatsFromFeed(rowFeed) {
    var result = {};
    var rowSplits = rowFeed.split(',');
    for (var i = 0; i < rowSplits.length; ++i) {
      // one stat would be of the form ' id204: 3'
      var oneStat = rowSplits[i];
      var id = oneStat.split(':')[0].replace(/^\s+/,'');  // skip spaces
      if (id.indexOf('id') != 0) {
        continue;
      }
      var value = parseFloat(oneStat.split(':')[1].replace(/^\s+/,''));  // skip spaces
      result[id] = value;
    }
    return result;
  }

  function getStringStatsFromFeed(rowFeed) {
    var result = {};
    var rowSplits = rowFeed.split(',');
    for (var i = 0; i < rowSplits.length; ++i) {
      // one stat would be of the form ' id204: 3' or  _6woj3: 375'.
      var oneStat = rowSplits[i];
      var id = oneStat.split(':')[0].replace(/^\s+/,'');  // skip spaces
      if (id.indexOf('id') != 0) {
        continue;
      }
      var value = oneStat.split(':')[1].replace(/^\s+/,'');  // skip spaces
      result[id] = value;
    }
    return result;
  }

  function createInfoWindow() {
    return $("<span><h2 style='font-family: arial; color: blue'><span id='stationName'></span></h2><a id='directions' class='directions'>מסלול לכאן</a><span class='nodirections' style='font-size: x-small; display: none; ' id='nodirections'>מיקומך אינו ידוע ליצירת מסלול</span><br><p><span style='border: solid 1px'><img src='greenbike.png' title='אופניים פנויים' ><span id='available_bikes'></span></span>&nbsp;&nbsp;<span style='border: solid 1px'><img src='docks.png' title='תחנות עגינה פנויות' ><span id='available_docks'> </span></span></p></span>").get()[0];
  }

  function resetInfoWindows() {
    for (id in stationsInfo) {
      var station = stationsInfo[id];
      var content = $(station.infowindow.getContent());
      content.find('#stationName').text((stations[id] && stations[id].displayName) ? stations[id].displayName : "");
      content.find('#directions').attr('href', 'javascript:cycleTo(\'' + id + '\')');
      content.find('#available_bikes').text(station.available_bikes);
      content.find('#available_docks').text(station.available_docks);
    }
  }

  function cycleTo(destinationId) {
    var nearestId = findClosestAvailableStation();
    if (!nearestId) {
      alert('לא מצליח למצוא תחנה קרובה ביותר');
      return;
    }
    showDirections(stations[destinationId].latLng, stations[nearestId].latLng);
  }

  function walkTo(destinationId) {
    showDiv('mapDiv');
    showDirections(stations[destinationId].latLng);
  }

  function showScoresAndCenterOn(id) {
    showDiv('mapDiv');
    var numStations = scoreSortedStations.length;
    for (var i = 0; i < numStations; ++i) {
      var station = scoreSortedStations[i];
      var marker = stationsInfo[station.id].marker;
      var file = (i <= numStations * 0.25 ? 'green.png' : (i <= 0.75 * numStations ? 'yellow.gif': 'red.gif'));
      marker.setIcon(new google.maps.MarkerImage(file, new google.maps.Size(20, 20)));
    }
    map.setCenter(stations[id].latLng);
    map.setZoom(Math.max(map.getZoom(), 17));
    setMapTitle('תחנות טובות, תחנות רעות');
  }

  function showDirections(destination, waypoint) {
    var CYCLING_SPEED = 7.0;  // KM/H
    var service = new google.maps.DirectionsService();
    service.route({origin: userPosition, destination: destination, travelMode: google.maps.TravelMode.WALKING, unitSystem: google.maps.UnitSystem.METRIC, waypoints: waypoint ? [{location: waypoint}] : undefined },
      function(result, status) {
        if (status != google.maps.DirectionsStatus.OK) {
          alert('בעיה בשליחת ההוראות');
          return;
        }
        if (waypoint) {  // show cycling info
		      var route = result.routes[0];
		      var distance = route.legs[route.legs.length-1].distance.value;
		      var middleLatLng = google.maps.geometry.spherical.interpolate(waypoint, destination, 0.5);
		      var placeToPutMessageLatLng = findMiddlePointToPutMessage(middleLatLng, route.legs[1].steps);
		      var durationMins = Math.round((distance / 1000 /* m->km */) / CYCLING_SPEED * 60 /* h->m */);
		      var routeContent = '<b>מרחק: ' + Math.round(distance/100) / 10 + " ק\"מ" + '<br/>זמן רכיבה: ' + durationMins + " דק'</b>";
		      openInfoWindow && openInfoWindow.close();
		      openInfoWindow = new google.maps.InfoWindow({content: routeContent, position: placeToPutMessageLatLng});
		      openInfoWindow.open(map);
        }
	      directionsRenderer.setDirections(result);
	      directionsRenderer.setMap(map);
      }
    );
  }

  function findClosestAvailableStation() {
    if (!userPosition) {
      return null;
    }
    var minDistance = Number.MAX_VALUE;
    var minId;
  	for (id in stations) {
      if (!stationsInfo[id] || !stationsInfo[id].available_bikes) {
        continue;
      }
      var station = stations[id];
      var curDistance = google.maps.geometry.spherical.computeDistanceBetween(
          userPosition, station.latLng);
      if (curDistance < minDistance) {
        minDistance = curDistance;
        minId = station.id;
      }
    }
    return minId;
  }

  function findMiddlePointToPutMessage(origin, steps) {
    var minDistance = Number.MAX_VALUE;
    var minIndex;
    for (var i = 0; i < steps.length; ++i) {
      var step = steps[i];
      var curDistance = google.maps.geometry.spherical.computeDistanceBetween(
          origin, step.start_location) +
                        google.maps.geometry.spherical.computeDistanceBetween(
          origin, step.end_location);
      if (curDistance < minDistance) {
        minDistance = curDistance;
        minIndex = i;
      }
      new google.maps.InfoWindow({content: 'start ' + i, position: step.start_position}).open(map);
      new google.maps.InfoWindow({content: 'end ' + i, position: step.end_position}).open(map);
    }
    return google.maps.geometry.spherical.interpolate(steps[minIndex].start_location, steps[minIndex].end_location, 0.5);
  }

  function timelyCallback(value) {
    clearTimeout(timeUpdateTimeout);
    var stats = [];
    var allKeys = {};
    var id;
    for (var i = 0; i < ROWS_PER_SAMPLE; ++i) {
      stats[i] = getFloatStatsFromFeed(value.feed.entry[i].content['$t']);
      for (id in stats[i]) {
        allKeys[id] = {};
      }
    }
    for (id in allKeys) {
      var sum = 0;
      var notEnoughBikes = 0;
      var notEnoughDocks = 0;
      for (var i = 0; i < stats.length; ++i) {
        if (id in stats[i]) {
          var value = stats[i][id];
          sum += value;
          switch (i) {
            case 1:  // no bikes
              notEnoughBikes += value;
              break;
            case 2:  // 1-2 bikes
              notEnoughBikes += value * ONE_OR_TWO_COMPARED_TO_NONE;
              break;
            case 3:  // no docks
              notEnoughDocks += value;
              break;
            case 4:  // 1-2 docks
              notEnoughDocks += value * ONE_OR_TWO_COMPARED_TO_NONE;
              break;
          }
        }
      }
      var image = 'greenbike.png';
      var statsContent = '';
      var statsArray = [];
      var isNotEnoughBikes = (notEnoughBikes / sum > RATIO_PROBLEM);
      var isNotEnoughDocks = (notEnoughDocks / sum > RATIO_PROBLEM);
      if (isNotEnoughBikes) {
        image = 'nobikes_0.png';
        statsArray.push(
    'ב- ' + Math.round(notEnoughBikes / sum * 100) + '% מהמקרים לא היו אופניים');
      }
      if (isNotEnoughDocks) {
        if (notEnoughDocks > notEnoughBikes) {
          image = 'nodocks_0.png';
        }
        statsArray.push(
    'ב- ' + Math.round(notEnoughDocks / sum * 100) + '% מהמקרים לא היו תחנות עגינה');
      }
      // Handle the special case where the station never became active.
      if (notEnoughBikes == notEnoughDocks && (notEnoughBikes + notEnoughDocks) >= 0.9 * sum) {
        image = 'dunno.png';
        statsArray = [ 'נראה שהתחנה מעולם לא היתה תקינה' ];
      }
      if (sum == 0 || (isNotEnoughBikes && isNotEnoughDocks)) {
        image = 'dunno.png';
      }
      statsContent += statsArray.join(' ו');

      var station = stationsInfo[id];
      changeMarkerUrl(station.marker, image);
      var content = (statsContent != '') ? (statsContent + ' בשעה זו') : '';
      $(station.infowindow.getContent()).append($('<br/><span>'+content+'</span>'));
    }
    // Turn all unknown stations into a question mark.
    for (id in stations) {
      if (!(id in allKeys)) {
        changeMarkerUrl(stationsInfo[id].marker, 'dunno.png'); 
      }
    }
    showSpinner(false);
  }

  function changeMarkerUrl(marker, aUrl) {
    if (!aUrl.indexOf) {
      alert(aUrl);
    }
    var oldIcon = marker.getIcon();
    marker.setIcon(new google.maps.MarkerImage(
        aUrl, undefined, undefined, oldIcon.scaledSize ? oldIcon.scaledSize : undefined));
  }

  function formatTime(updated) {
    var result = ' ';
    var now = new Date();
    if (now.getTime() - updated.getTime() < 6 /* mins */ * 60 * 1000) {
      return 'עכשיו';
    }
    result += updated.format('HH:MM');
    if (updated.format('d/m') != now.format('d/m')) {
      result += ' ' + updated.format('d/m') + ' ';
    }
    return result;
  }

  function onSetMapTitle() {
    setMapTitle('נכון ל' + formatTime(lastUpdateTime));
  }

  function getOrCreateStationInfo(id) {
    if (stationsInfo[id]) {
      if (!stationsInfo[id].marker.getPosition() && stations[id]) {
        stationsInfo[id].marker.setPosition(stations[id].latLng);
      }
      return stationsInfo[id];
    }
    var station = {};
    station.id = id;
    stationsInfo[id] = station;
    var marker = new google.maps.Marker({
        position: stations[id] ? stations[id].latLng : undefined,
        title: stations[id] ? stations[id].displayName : undefined,
        icon: new google.maps.MarkerImage('spinner.gif')
    });
    station.marker = marker;
    marker.setMap(map);
    var infowindow = new google.maps.InfoWindow({content: createInfoWindow()});
    station.infowindow = infowindow;
    google.maps.event.addListener(marker, 'click', function(marker,infowindow) {
      return function() {
        var showDirections = (userPosition != null);
        $(infowindow.getContent()).find('.directions').toggle(showDirections);
        $(infowindow.getContent()).find('.nodirections').toggle(!showDirections);
        infowindow.open(map, marker);
        openInfoWindow && openInfoWindow.close();
        openInfoWindow = infowindow;
      }
    }(marker, infowindow));
    return station;
  }

  function currentStatusCallback(value) {
      lastUpdateTime = new Date(value.entry.updated['$t']);
    onSetMapTitle(); 
    if (timeUpdateTimeout) {
      clearTimeout(timeUpdateTimeout);
    }
    timeUpdateTimeout = 
        setTimeout(onSetMapTitle, 
            new Date(lastUpdateTime.getTime() + 6.1 /* mins */ * 60 * 1000 - new Date().getTime()));
    currentStatus = getFloatStatsFromFeed(value.entry.content['$t']);
    for (id in currentStatus) {
      var status = currentStatus[id];
      var station = getOrCreateStationInfo(id);
      
      station.available_bikes = status % 100;
      station.available_docks = Math.floor(status / 100);
      if (station.stats) {
        delete station['stats'];
      }
    }
    showSpinner(false);
    bikeStatusLoaded = true;
    resetInfoWindows();
    maybeUpdateStationsUI();
    maybeUpdateDistanceSortedStations();
  }

  function maybeUpdateStationsUI() {
    if (!namesLoaded || !locationsLoaded || !bikeStatusLoaded) {
      return;
    }
    for (id in stations) {
      var station = stationsInfo[id];
      var image;
      if (station.available_bikes == 0 && station.available_docks == 0) {
        image = 'dunno.png';
      } else if (station.available_bikes in [0,1,2]) {
        image = 'nobikes_' + station.available_bikes + '.png';
      } else if (station.available_docks in [0,1,2]) {
        image = 'nodocks_' + station.available_docks + '.png';
      } else {
        image = 'greenbike.png';
      }
      changeMarkerUrl(station.marker, image);
      station.marker.setTitle(stations[id].displayName + ', אופניים: ' + station.available_bikes + ' תחנות: ' + station.available_docks);
    }
    getStationRanking();
  }

  function createStationRankingIFrame() {
    if ($('#stationRankFrame').length == 0) {
      $('#stationRankPlaceholder').append("<iframe id='stationRankFrame' width='350' height='640' src='https://docs.google.com/spreadsheet/pub?hl=en_US&hl=en_US&key=0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E&single=true&gid=9&output=html'></iframe>");
    }
  }

  function getCurrentStationsStatus() {
    var CURRENT_STATUS_ROW_ID = '25ncrc';

    hidePrediction();
    readRowFromSpreadsheet(CURRENT_STATUS_ROW_ID, 'currentStatusCallback');

    showSpinner(true);
    for (id in stationsInfo) {
      if (stationsInfo[id].marker) {
        changeMarkerUrl(stationsInfo[id].marker, 'spinner.gif'); 
      }
    }

    $('#station_ok').text('תחנה תקינה');
    $('#station_nobikes').text('אין אופניים');
    $('#station_nodocks').text('אין תחנות עגינה');
    $('#station_noinfo').text('אין פרטים');
  }

  function timeUp(time) {
    if (time == 24) {   // Special case
      time = 0;
    }
    var baseRow = START_ROW + ROWS_PER_SAMPLE * time * 4 - 1;  // first row considered headers

    for (id in stations) {
      changeMarkerUrl(stationsInfo[id].marker, 'spinner.gif');
    }
    showSpinner(true);
    var script = document.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.setAttribute('src', 'https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic?alt=json-in-script&callback=timelyCallback&start-index=' + baseRow + '&max-results=' + ROWS_PER_SAMPLE);
    $('head').append(script);

    $('#station_ok').text('תחנה תקינה בד"כ');
    $('#station_nobikes').text('לעתים אין אופניים');
    $('#station_nodocks').text('לעתים תחנה מלאה');
    $('#station_noinfo').text('תחנה בעייתית או שאין פרטים');
    setMapTitle('תחזית לשעה ' + numericTimeToHumanTime(time));
  }

  function stationLocationsCallback(stationsInfo) {
    // longLats of the form:
    // id202: 32.122; 34.818, id112: 32.113; 34.801, ....
    var longLatsString = stationsInfo.entry.content['$t'];  
    var longLats = getStringStatsFromFeed(longLatsString);

    for (var id in longLats) {
      var station;
      if (stations[id]) {
        station = stations[id];
      } else {
        station = {id: id};
        stations[id] = station;
      }
      // We create the lat, lng fields for presistency, and the latLng for usage throughout the code.
      station.lat = parseFloat(longLats[id].split(';')[0]); 
      station.lng = parseFloat(longLats[id].split(';')[1]); 
      station.latLng = new google.maps.LatLng(station.lat, station.lng);
    }
    locationsLoaded = true;
    if (namesLoaded) {
      maybePrepareMap();
    }
  }

  function stationNamesCallback(stationsInfo) {
    // stationNames is of the form:
    // id202: אהרון בקר 15, id112: אונברסיטה איינשטיין 78, ...
    var namesString = stationsInfo.entry.content['$t'];  
    var names = getStringStatsFromFeed(namesString);

    for (var id in names) {
      var station;
      if (stations[id]) {
        station = stations[id];
      } else {
        station = {id: id};
        stations[id] = station;
      }
      station.displayName = names[id];
    }
    namesLoaded = true;
    if (locationsLoaded) {
      maybePrepareMap();
    }
  }

  function populateStationDistanceTable() {
      $('#stationDistanceTable').html('');
      for (var i=0; i < Math.min(4, distanceSortedStations.length); ++i) {
        var station = distanceSortedStations[i];
        var line = $("<tr class='stationInTable' onclick='javascript:walkTo(\"" + station.id +"\")'>" +
            "<td class = 'stationInTableName stationInTableCell'>" + station.displayName + 
            "</td><td class = 'stationInTableCell'><img src='greenbike.png' width='17' height='17' />" + stationsInfo[station.id].available_bikes +
            "</td><td class ='stationInTableCell'><img src='docks.png' width='17' height='17' />" + stationsInfo[station.id].available_docks +
            "</td><td class='stationInTableCell'><img src='walk.gif' width='17' height='17' />" + station.distance + ' מ\'</td></tr>');
        $('#stationDistanceTable').append(line);
      }
  }

  function populateStationScoresTable(scoreSortedStations) {
    $('#stationScoreTable').html('');
    for (var i = 0; i < scoreSortedStations.length; ++i) {
      var img = null;
      if (i == 0) {
        img = 'green.png';
      } else if (i == Math.round(0.25 * scoreSortedStations.length)) {
        img = 'yellow.gif';
      } else if (i == Math.round(0.75 * scoreSortedStations.length)) {
        img = 'red.gif';
      }
      var station = scoreSortedStations[i];
      var line = $("<tr class='stationInScoreTable' onclick='javascript:showScoresAndCenterOn(&apos;" + station.id +"&apos;)'>" +
          "<td>" + (img ? '<img width=25 height=25 src="' + img + '"/>' : '') + '</td>' +
          "<td class = 'stationInTableName stationInTableCell'>" + station.displayName + 
          "</td><td class = 'stationInTableCell'>" + station.score +
          "</td></td></tr>");
      $('#stationScoreTable').append(line);
    }
  }

  function maybeUpdateDistanceSortedStations() {
    if (!namesLoaded || !locationsLoaded || !bikeStatusLoaded || !userPosition) {
      return;
    }
    distanceSortedStations = [];
    for (var id in stations) {
      var station = stations[id];
      station.distance = Math.round(google.maps.geometry.spherical.computeDistanceBetween(
	        userPosition, station.latLng));
      distanceSortedStations.push(station);
    }
	  distanceSortedStations.sort(function(a,b) {
	    return a.distance - b.distance;
	  });
    populateStationDistanceTable();
  }

  function maybePrepareMap() {
    if (!namesLoaded || !locationsLoaded) {
      return;
    }
    for (var id in stations) {
      getOrCreateStationInfo(id);
    }
    resetInfoWindows();
    maybeUpdateStationsUI();
    maybeUpdateDistanceSortedStations();
  }


  function setMapTitle(titleString) {
    var title = $('#mapTitle');
    title.text(titleString);
 
    title.css('font-size', titleString.length <= 12 
        ? 'medium' 
        : (titleString.length <= 18 ? 'small' : 'x-small'));
  }

  function showSpinner(show) {
    if (show) {
      $('#mapTitle').hide();
      $('#spinner').show(); 
    } else {
      $('#mapTitle').show();
      $('#spinner').hide(); 
    }
  }

  function getUserLocation() {
    if (navigator.geolocation) {
    	navigator.geolocation.getCurrentPosition(
        function(position) {  // success
          userPosition = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
          if (tlvBounds.contains(userPosition)) {
            map.setCenter(userPosition);
            if (myPosCircle) {
              myPosCircle.setMap(null);
            }
            if (position.coords.accuracy < 500) {
              myPosCircle = new google.maps.Circle({map: map, center: userPosition, fillColor: 'darkgray', 
                  fillOpacity: 0.4, strokeColor: 'black', strokeOpacity: 0.7, radius: parseInt(position.coords.accuracy)});
            }
            if (map.getZoom() < 15) {
              map.setZoom(15);
            }
            maybeUpdateDistanceSortedStations();
          } else {  // userPosition is outside TLV
            map.fitBounds(tlvBounds);
            userPosition = null;
          }
        },
        function(error) {  // error
            map.fitBounds(tlvBounds);
            userPosition = null;
        },  
        { enableHighAccuracy: true, timeout: 20000, maximumAge: 60000 });
    }
  }

  function showDiv(whatToShow) {
    var divs = ['loadingDiv', 'mapDiv', 'stationRankingDiv', 'aboutDiv', 'stationDistanceDiv'];
    for (i in divs) {
      var div = divs[i];
      $('#' + div).toggle(whatToShow == div);
    }
  }

  function loadStationsFromWeb() {
    var LOCATIONS_ROW_ID = '205aqv';
    var NAMES_ROW_ID = 'cn6ca';

    readRowFromSpreadsheet(LOCATIONS_ROW_ID, 'stationLocationsCallback');
    readRowFromSpreadsheet(NAMES_ROW_ID, 'stationNamesCallback');
    getCurrentStationsStatus();
  }

  function initialize() {
    // Create map.
    adjustToScreenSize();
    showDiv('mapDiv');
    var latlng = new google.maps.LatLng(32.066792, 34.777694);  // Merkaz Tel Aviv
    var mapOptions = {
      zoom: 16,
      minZoom: 10,
      center: latlng,
      mapTypeControl: false,
      scaleControl: true,
      rotateControl: false,
      streetViewControl: false,
      zoomControl: true,
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map = new google.maps.Map(document.getElementById('map_canvas'),
        mapOptions);
    google.maps.event.addListener(map, 'tilesloaded', function() {
      $('#loadingDiv').remove();
		});

    google.maps.event.addListener(map, 'click', function() {
        openInfoWindow && openInfoWindow.close();
        openInfoWindow = null;
		});

    google.maps.event.addListener(map, 'bounds_changed', function() {
        var bounds = map.getBounds();
        for (id in stations) {
          if (!stationsInfo[id] || !(stationsInfo[id].marker)) {
            continue;
          }
          var marker = stationsInfo[id].marker;
          var isVisible = marker.getVisible();
          var shouldBeVisible = bounds.contains(stations[id].latLng);
          if (isVisible != shouldBeVisible) {
            marker.setVisible(shouldBeVisible);
          }
        }
    });

    google.maps.event.addListener(map, 'zoom_changed', function() {
        var size = map.getZoom() <= 13 ? 18 : (map.getZoom <= 16 ? 24 : 36);
        if (oldSize == size) {
          return;  // no need to change the size of the icons.
        }
        oldSize = size;

        for (id in stationsInfo) {
          var oldIcon = stationsInfo[id].marker.getIcon();
          stationsInfo[id].marker.setIcon(
              new google.maps.MarkerImage(oldIcon.url, undefined, undefined, undefined, new google.maps.Size(size, size)));
        }
		});

    // Retrieve stations.
    loadStationsFromWeb();
 
    // Get user location.
    getUserLocation();

		var mapTitle = $('<div>');
		mapTitle.css({'color': 'black', 'font-weight': 'bold', 'font-size': 'medium', 'width': '180px' });
		mapTitle.attr('id', 'mapTitle');
    mapTitle.click(getCurrentStationsStatus);

    var imageDiv = $('<div>');
    imageDiv.css({'float':'right'});

    var refreshImg = $('<img>');
    refreshImg.attr({'src': 'refresh.png', 'width': '21', 'height': '21', 'title':'רענן'});
    refreshImg.css({'vertical-align': 'middle', 'padding': '1px', 'border-right': 'solid black 1px', 
      'float': 'left'});
    refreshImg.click(getCurrentStationsStatus);

    var myLocImg = $('<img>');
    myLocImg.attr({'src': 'myloc.png', 'width': '21', 'height': '21', 'title':'המקום שלי'});
    myLocImg.css({ 'vertical-align': 'middle', 'padding': '1px', 'border-left': 'solid black 1px'});
    myLocImg.click(function() { getUserLocation() });

    var listImg = $('<img>');
    listImg.attr({'src': 'list.png', 'width': '21', 'height': '21', 'title':'תחנות קרובות'});
    listImg.css({ 'vertical-align': 'middle', 'padding': '1px', 'border-left': 'solid black 1px'});
    listImg.click(function() { showDiv('stationDistanceDiv') });

    imageDiv.append(myLocImg, listImg);

		var myTextDiv = $('<div>');
		myTextDiv.css({'background': 'white', 'opacity': 0.7, 'width': '190px', 'height' : '20px', 'border': 'solid black 1px', 'padding': '5px', 'text-align': 'center', 'cursor': 'pointer' });
		var spinner = $('<img>');
		spinner.attr({'src': 'spinner.gif', 'id': 'spinner', 'height': '19px', 'width': '19px'});
    myTextDiv.append(refreshImg, imageDiv, mapTitle, spinner);

		map.controls[google.maps.ControlPosition.TOP_LEFT].push(myTextDiv.get()[0]);
    geocoder = new google.maps.Geocoder();
    initAutocomplete();
  }

function adjustToScreenSize() {
  if (screen.availWidth > 800) {
     $('#legend').show();
     $('#upperBar').show();
     $('#mainTable').height('75%');
     $('#mapTd').width('80%');
     $('#lowerText').show();
  }
}

function readRowFromSpreadsheet(lineId, callback) {
  var link = 
      'https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic/' + 
      lineId + '?alt=json-in-script&callback=' + callback;
  var script = $('<script>');
  script.attr({type: 'text/javascript', src: link});
  $('head').append(script);
}

function stationRankingCallback(stationsInfo) {
  // stationNames is of the form:
  // id202: 0.923, id115: 0.115, ....
  var scoresString = stationsInfo.entry.content['$t'];  
  var scores = getFloatStatsFromFeed(scoresString);

  scoreSortedStations = [];
  for (var id in scores) {
    stations[id].score = Math.round(scores[id] * 100 * 10) / 10;  // Percent, rounded to promiles.
    scoreSortedStations.push(stations[id]);
  }
  scoreSortedStations.sort(function(a,b) {
    return b.score - a.score;
  });
  populateStationScoresTable(scoreSortedStations);
}

function getStationRanking() {
  if (scoresLoaded) {
    return;
  }
  scoresLoaded = true;
  var STATION_RANKING_ROW_ID = '20ax0j';
  readRowFromSpreadsheet(STATION_RANKING_ROW_ID, 'stationRankingCallback');
}

function initAutocomplete() {
  placeService = new google.maps.places.PlacesService(map);
  var text = $('#locate').get()[0];
  var autocomplete = new google.maps.places.Autocomplete(text, {bounds: tlvBounds, types: ['geocode']});
}

function search() {
  var location = $('#locate').val();
  geocoder.geocode({bounds: tlvBounds, address: location}, function(result, status) {
    if (status == google.maps.GeocoderStatus.OK) {
      var placeLocation = result[0].geometry.location;
		  openInfoWindow && openInfoWindow.close();
      openInfoWindow = new google.maps.InfoWindow({content: location, position: placeLocation});
      openInfoWindow.open(map);

		  map.setCenter(placeLocation);
    }
  });
}

function togglePrediction() {
  $('#prediction').toggle();
//  alert ('display is ' + $('#prediction').css('display') + ', type is ' + $('#hour').get()[.attr('type'));
  if ($('#prediction').css('display') != 'none' && $('#hour').get()[0].type != 'range') {
    alert('הדפדפן שלך לא חדש מספיק ואינו תומך בתכונות הנדרשות לתחזית. על מנת לראות תחזית מומלץ להשתמש בדפדפן חדש כמו כרום של גוגל');
    $('#prediction').hide();
  }
}

function hidePrediction() {
  $('#prediction').hide();
}

