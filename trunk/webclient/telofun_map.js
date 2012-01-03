// Constants.
  var START_ROW = 4;
  var ROWS_PER_SAMPLE = 5;
  var RATIO_PROBLEM = 0.15;  // ratio above which there is a problem with the station
  var ONE_OR_TWO_COMPARED_TO_NONE = 0.25;  // how to weigh 1-2 bikes/docks compared to none
  var neBound = new google.maps.LatLng(32.13194, 34.847946);
  var swBound = new google.maps.LatLng(32.030381, 34.739285);
  var tlvBounds = new google.maps.LatLngBounds(swBound, neBound);

// Global vars.
  var stations = {};  // static information about stations, mapped by id.
  var stationsInfo = {};  // dynamic information about stations, mapped by id.
  var openInfoWindow = null;
  var map = {};
  var userPosition = null;
  var myPosCircle = undefined;
  var timeUpdateTimeout = undefined;
  var lastUpdateTime = undefined;
  var directionsRenderer = new google.maps.DirectionsRenderer();
  var loadedStationsFromWeb = false;
  var namesLoaded = false;
  var locationsLoaded = false;

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

  function getIntStatsFromFeed(rowFeed) {
    var result = {};
    var rowSplits = rowFeed.split(',');
    for (var i = 0; i < rowSplits.length; ++i) {
      // one stat would be of the form " id204: 3" or  _6woj3: 375".
      var oneStat = rowSplits[i];
      var id = oneStat.split(':')[0].replace(/^\s+/,'');  // skip spaces
      if (id.indexOf('id') != 0) {
        continue;
      }
      var value = parseInt(oneStat.split(':')[1].replace(/^\s+/,''));  // skip spaces
      result[id] = value;
    }
    return result;
  }

  function getStringStatsFromFeed(rowFeed) {
    var result = {};
    var rowSplits = rowFeed.split(',');
    for (var i = 0; i < rowSplits.length; ++i) {
      // one stat would be of the form " id204: 3" or  _6woj3: 375".
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

  function resetInfoWindows() {
    for (key in stationsInfo) {
      var station = stationsInfo[key];
      var content = "<span><h2 style='font-family: arial; color: blue'>" +
          (stations[key] ? stations[key].displayName : "") + "</h2>";
      content += "<a class='directions' style='display: none;' href='javascript:directionsTo(\"" + station.id + "\");'>מסלול לכאן</a>";
      content += "<span class='nodirections' style='font-size: x-small'>מיקומך אינו ידוע ליצירת מסלול</span>";
      content += '<br/></span>';
      station.infowindow.setContent($(content).get()[0]);
    }
  }

  function directionsTo(destinationId) {
    var nearestId = findClosestStation();
    if (!nearestId) {
      alert('לא מצליח למצוא תחנה קרובה ביותר');
      return;
    }
    showDirections(stations[nearestId].latLng, stations[destinationId].latLng);
  }

  function showDirections(origin, destination) {
    var service = new google.maps.DirectionsService();
    service.route({origin: origin, destination: destination, travelMode: google.maps.TravelMode.WALKING, unitSystem: google.maps.UnitSystem.METRIC},
      function(result, status) {
        if (status != google.maps.DirectionsStatus.OK) {
          alert('בעיה בשליחת ההוראות');
          return;
        }
        directionsRenderer.setMap(map);
        directionsRenderer.setDirections(result);
      }
    );
  }

  function findClosestStation() {
    if (!userPosition) {
      return null;
    }
    var minDistance = Number.MAX_VALUE;
    var minId;
  	for (key in stations) {
      var station = stations[key];
      var curDistance = google.maps.geometry.spherical.computeDistanceBetween(
          userPosition, station.latLng);
      if (curDistance < minDistance) {
        minDistance = curDistance;
        minId = station.id;
      }
    }
    return minId;
  }

  function timelyCallback(value) {
    resetInfoWindows();
    clearTimeout(timeUpdateTimeout);
    var stats = [];
    var allKeys = {};
    var key;
    for (var i = 0; i < ROWS_PER_SAMPLE; ++i) {
      stats[i] = getIntStatsFromFeed(value.feed.entry[i].content["$t"]);
      for (key in stats[i]) {
        allKeys[key] = {};
      }
    }
    for (key in allKeys) {
      var sum = 0;
      var notEnoughBikes = 0;
      var notEnoughDocks = 0;
      for (var i = 0; i < stats.length; ++i) {
        if (key in stats[i]) {
          var value = stats[i][key];
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
      var statsContent = "";
      var statsArray = [];
      var isNotEnoughBikes = (notEnoughBikes / sum > RATIO_PROBLEM);
      var isNotEnoughDocks = (notEnoughDocks / sum > RATIO_PROBLEM);
      if (isNotEnoughBikes) {
        image = 'nobikes.png';
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
      statsContent += statsArray.join(" ו");

      var station = stationsInfo[key];
      station.marker.setIcon(image);
      var content = (statsContent != "") ? (statsContent + " בשעה זו") : "";
      $(station.infowindow.getContent()).append($(content));
    }
    // Turn all unknown stations into a question mark.
    for (key in stations) {
      if (!(key in allKeys)) {
        stationsInfo[key].marker.setIcon('dunno.png');
      }
    }
    showSpinner(false);
  }

  function formatTime(updated) {
    var result = " ";
    var now = new Date();
    if (now.getTime() - updated.getTime() < 6 /* mins */ * 60 * 1000) {
      return "עכשיו";
    }
    result += updated.format('HH:MM');
    if (updated.format('d/m') != now.format('d/m')) {
      result += ' ' + updated.format('d/m') + ' ';
    }
    return result;
  }

  function onSetMapTitle() {
    setMapTitle('המצב נכון ל' + formatTime(lastUpdateTime));
  }

  function getOrCreateStationInfo(id) {
    if (!stations[id]) {
      loadStationsFromWeb();
    }
    if (stationsInfo[id]) {
      return stationsInfo[id];
    }
    var station = {};
    station.id = id;
    stationsInfo[id] = station;
    var marker = new google.maps.Marker({
        position: stations[id] ? stations[id].latLng : undefined,
        title: stations[id] ? stations[id].displayName : undefined,
    });
    station.marker = marker;
    marker.setMap(map);
    var infowindow = new google.maps.InfoWindow({});
    station.infowindow = infowindow;
    google.maps.event.addListener(marker, 'click', function(marker,infowindow) {
      return function() {
        if (openInfoWindow) {
          openInfoWindow.close();
        }
        var showDirections = (userPosition != null);
        $(infowindow.content).find('.directions').toggle(showDirections);
        $(infowindow.content).find('.nodirections').toggle(!showDirections);
        infowindow.open(map, marker);
        openInfoWindow = infowindow;
      }
    }(marker, infowindow));
    return station;
  }

  function currentStatusCallback(value) {
//      lastUpdateTime = new Date(value.feed.updated["$t"]);
      lastUpdateTime = new Date(value.entry.updated["$t"]);
    onSetMapTitle(); 
    if (timeUpdateTimeout) {
      clearTimeout(timeUpdateTimeout);
    }
    timeUpdateTimeout = setTimeout(onSetMapTitle, new Date(lastUpdateTime.getTime() + 6.1 /* mins */ * 60 * 1000 - new Date().getTime()));
    resetInfoWindows();
    currentStatus = getIntStatsFromFeed(value.entry.content["$t"]);
    for (key in currentStatus) {
      var status = currentStatus[key];
      var station = getOrCreateStationInfo(key);
      
      station.available_bikes = status % 100;
      station.available_docks = Math.floor(status / 100);
      if (station.stats) {
        delete station["stats"];
      }

      var image;
      if (status == 0) {
        image = 'dunno.png';
      } else if (station.available_bikes in [0,1,2]) {
        image = 'nobikes_' + station.available_bikes + '.png';
      } else if (station.available_docks in [0,1,2]) {
        image = 'nodocks_' + station.available_docks + '.png';
      } else {
        image = 'greenbike.png';
      }
      station.marker.setIcon(image);
      station.marker.setTitle(station.displayName + ', אופניים: ' + station.available_bikes + ' תחנות: ' + station.available_docks);
      $(station.infowindow.getContent()).append($( 
        '<p>' + '<span style="border: solid 1px"><img src="greenbike.png" title="אופניים פנויים" />' + station.available_bikes + "</span>" +  
                '&nbsp;&nbsp;<span style="border: solid 1px"><img src="docks.png" title="תחנות עגינה פנויות" />' + station.available_docks + '</span></p>'));
    }
    showSpinner(false);
    createStationRankingIFrame();
  }

  function createStationRankingIFrame() {
    if ($('#stationRankFrame').length == 0) {
      $('#stationRankPlaceholder').append("<iframe id='stationRankFrame' width='350' height='640' src='https://docs.google.com/spreadsheet/pub?hl=en_US&hl=en_US&key=0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E&single=true&gid=9&output=html'></iframe>");
    }
  }

  function nowStatus() {
    var CURRENT_STATUS_ROW = 484 - 1;
    var OLD_LINK = 'https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic?alt=json-in-script&callback=currentStatusCallback&start-index=' + CURRENT_STATUS_ROW + '&max-results=' + 1;
    var CURRENT_STATUS_ROW_ID = '25ncrc';
    var NEW_LINK = 'https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic/' + CURRENT_STATUS_ROW_ID + 
      '?alt=json-in-script&callback=currentStatusCallback';

    showSpinner(true);
    for (key in stationsInfo) {
      if (stationsInfo[key].marker) {
        stationsInfo[key].marker.setIcon('spinner.gif');
      }
    }

    var script = document.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.setAttribute('src', NEW_LINK);
    $('head').append(script);

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

    for (key in stations) {
      stationsInfo[key].marker.setIcon('spinner.gif');
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

/*
  function stationsCallback(stationsInfo) {
    // longLats of the form:
    // id202: 32.122; 34.818, id112: 32.113; 34.801, ....

    // stationNames is of the form:
    // id202: אהרון בקר 15, id112: אונברסיטה איינשטיין 78, ...
    var longLats = stationsInfo.feed.entry[0].content["$t"];  
    longLats = longLats.substring(longLats.indexOf('id')).split(',');  // skip to the first "id" and split
    var stationNames = stationsInfo.feed.entry[1].content["$t"];
    stationNames = stationNames.substring(stationNames.indexOf('id')).split(',');  // skip to the first "id" and split
    if (longLats.length != stationNames.length) {
      alert('error in data (longLats.length != stationNames.length)');
      exit;
    }
    for (var i = 0; i < longLats.length; i++) {
      var longLatsSplits = longLats[i].split(':');
      var idName = longLatsSplits[0].replace(/^\s+/,"");  // trim
      if (idName.indexOf('id') != 0)  // spreadsheet has prefixing columns
        continue;
      var newStation = {};
      newStation.id = idName;
      // We create the lat, lng fields for presistency, and the latLng for usage throughout the code.
      newStation.lat = parseFloat(longLatsSplits[1].split(';')[0]); 
      newStation.lng = parseFloat(longLatsSplits[1].split(';')[1]); 
      newStation.latLng = new google.maps.LatLng(newStation.lat, newStation.lng);
      newStation.displayName = stationNames[i].split(':')[1];
      stations[idName] = newStation;
      // Create the appropriate dynamic station.
      getOrCreateStationInfo(idName);
    }
    localStorage['stations'] = JSON.stringify(stations);
    resetInfoWindows();
    nowStatus();
  }
*/
  function stationLocationsCallback(stationsInfo) {
    // longLats of the form:
    // id202: 32.122; 34.818, id112: 32.113; 34.801, ....
    var longLatsString = stationsInfo.entry.content["$t"];  
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
      getOrCreateStationInfo(id);
    }
//    localStorage['stations'] = JSON.stringify(stations);
    locationsLoaded = true;
    if (namesLoaded) {
      resetInfoWindows();
      nowStatus();
    }
  }

  function stationNamesCallback(stationsInfo) {
    // stationNames is of the form:
    // id202: אהרון בקר 15, id112: אונברסיטה איינשטיין 78, ...
    var namesString = stationsInfo.entry.content["$t"];  
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
      getOrCreateStationInfo(id);
    }
//    localStorage['stations'] = JSON.stringify(stations);
    namesLoaded = true;
    if (locationsLoaded) {
      resetInfoWindows();
      nowStatus();
    }
  }

  function setMapTitle(title) {
    $('#mapTitle').text(title);
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
                  fillOpacity: 0.4, strokeColor: 'black', radius: parseInt(position.coords.accuracy)});
            }
            if (map.getZoom() < 15) {
              map.setZoom(15);
            }
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

  function showMap(show) {
    var itemToShow = show ? '#mapDiv' : '#stationRankingDiv';
    var itemToHide = !show ? '#mapDiv' : '#stationRankingDiv';
    $(itemToShow).show();
    $(itemToHide).hide();
  }

  function loadStationsFromWeb() {
    var LOCATIONS_ROW_ID = '205aqv';
    var NAMES_ROW_ID = 'cn6ca';
    var NEW_LINK_LOCATIONS = 'https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic/' + LOCATIONS_ROW_ID + 
      '?alt=json-in-script&callback=stationLocationsCallback';  // does not work yet - retrieves only 
    var NEW_LINK_NAMES = 'https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic/' + NAMES_ROW_ID + 
      '?alt=json-in-script&callback=stationNamesCallback';  // does not work yet - retrieves only 
/*    var OLD_LINK = 'https://spreadsheets.google.com/feeds/list/0AoOjWPdv2TXodHlMTTFrakJKR2F6cldJTGktQnNXV0E/od6/public/basic?alt=json-in-script&callback=stationsCallback&max-results=2';*/
    if (loadedStationsFromWeb) {
      return;
    }
    loadedStationsFromWeb = true;
/*    var script = document.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.setAttribute('src', OLD_LINK);
    $('head').append(script);*/

    var script;
    script = document.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.setAttribute('src', NEW_LINK_LOCATIONS);
    $('head').append(script);

    script = document.createElement('script');
    script.setAttribute('type', 'text/javascript');
    script.setAttribute('src', NEW_LINK_NAMES);
    $('head').append(script);
  }

  function loadStationsFromLocalCache() {
    if (!localStorage['stations']) {
      return false;
    }
    stations = JSON.parse(localStorage['stations']);
    // latLng's aren't created correctly, recreate them.
    for (id in stations) {
      var station = stations[id];
      if (!station.id || !station.lat || !station.lng) {
        return false;
      }
      station.latLng = new google.maps.LatLng(station.lat, station.lng);
      // Create the dynamic station object.
      getOrCreateStationInfo(id);
    }
    return true;
  }

  function initialize() {
    // Create map.
    adjustForSmallScreen();
    var latlng = new google.maps.LatLng(32.066792, 34.777694);  // Merkaz Tel Aviv
    var mapOptions = {
      zoom: 16,
      center: latlng,
      mapTypeControl: false,
      scaleControl: true,
      streetViewControl: false,
      zoomControl: true,
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map = new google.maps.Map(document.getElementById("map_canvas"),
        mapOptions);
    google.maps.event.addListener(map, 'click', function() {
        if (openInfoWindow) {
          openInfoWindow.close();
          openInfoWindow = null;
        }
		});

    // Retrieve stations.
/*
    if (loadStationsFromLocalCache()) {
      resetInfoWindows();
      nowStatus();
    } else {
      loadStationsFromWeb();
    }
*/
    loadStationsFromWeb();
 
    // Get user location.
    getUserLocation();

		var myTitle = $('<div>');
		myTitle.css({'color': 'black', 'font-weight': 'bold', 'font-size': 'medium', 'width': '180px'});
		myTitle.attr('id', 'mapTitle');

    var myLocImg = $('<img>');
    myLocImg.attr({'src': 'myloc.png', 'width': '20', 'height': '20', 'title':'המקום שלי'});
    myLocImg.css({'float': 'right', 'vertical-align': 'middle', 'padding-left': '2px', 'border-left': 'solid black 1px'});
    myLocImg.click(function() { getUserLocation() });

		var myTextDiv = $('<div>');
		myTextDiv.css({'background': 'white', 'opacity': 0.7, 'width': '190px', 'height' : '20px', 'border': 'solid black 1px', 'padding': '5px', 'text-align': 'center' });
		var spinner = $('<img>');
		spinner.attr({'src': 'spinner.gif', 'id': 'spinner', 'height': '19px', 'width': '19px'});
    myTextDiv.append(myLocImg, myTitle, spinner);

		map.controls[google.maps.ControlPosition.TOP_LEFT].push(myTextDiv.get()[0]);
  }

function adjustForSmallScreen() {
  if (screen.availWidth <= 800) {
     $('#legend').hide();
     $('#upperBar').hide();
     $('#mainTable').width('100%');
     $('#mainTable').height('90%');
     $('#mapTd').width('100%');
     $('#lowerText').remove();
  }
}
