const core = require('/data-hub/5/mapping-functions/core.sjs');
const test = require("/test/test-helper.xqy");
const serverTimezone = sem.timezoneString(fn.currentDateTime());

function testParseDateTime() {
let expectedDateTime = xs.dateTime(`2014-01-06T18:13:50${serverTimezone}`);
return [
    test.assertTrue(expectedDateTime.eq(core.parseDateTime("DD/MM/YYYY-hh:mm:ss","06/01/2014-18:13:50"))),
    test.assertTrue(expectedDateTime.eq(core.parseDateTime("DD/MM/YYYY hh:mm:ss","06/01/2014 18:13:50"))),
    test.assertTrue(expectedDateTime.eq(core.parseDateTime("YYYYMMDDThhmmss","20140106T181350"))),
    test.assertTrue(expectedDateTime.eq(core.parseDateTime("YYYY/MM/DD-hh:mm:ss","2014/01/06-18:13:50"))),
    test.assertTrue(expectedDateTime.eq(core.parseDateTime("YYYY/MM/DD hh:mm:ss","2014/01/06 18:13:50"))),
    test.assertThrowsError(xdmp.function(xs.QName("dt.parseDateTime")), "YYYY/MM/DDThh:mm:ss","2014/01/06T18:13:50", null)
  ];
}

function testParseDate() {
return [
    test.assertTrue(xs.date("2014-01-06").eq(core.parseDate("MM/DD/YYYY","01/06/2014"))),
    test.assertTrue(xs.date("2014-01-06").eq(core.parseDate("DD/MM/YYYY","06/01/2014"))),
    test.assertTrue(xs.date("2014-01-06").eq(core.parseDate("MM.DD.YYYY","01.06.2014"))),
    test.assertTrue(xs.date("2014-01-06").eq(core.parseDate("DD.MM.YYYY","06.01.2014"))),
    test.assertTrue(xs.date("2014-01-06").eq(core.parseDate("YYYYMMDD","20140106"))),
    test.assertTrue(xs.date("2014-01-06").eq(core.parseDate("Mon DD, YYYY","Jan 06, 2014")))
  ];
}
[].concat(testParseDateTime()).concat(testParseDate());