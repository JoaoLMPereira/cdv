CDV - Community Data Validator
==============================


About
-----


CDV adds the ability of creating validation tests on the Pentaho BA server for the
purpose of verifying both the integrity of the server itself and also the data being
used by the server.


Motivation
----------


Very often we want that several questions related to the data would be answered before we noticed it: 

* Do we have up to date data?
* Can we trust the data?
* How long did the queries take to run?
* Do we have wrong data? (duplicated users in community)
* Do we have a big number of 'unknowns'?(tk=1 in DW)
* Do we have peaks or valleys in the data? (due to double process or no process)
* Is the data stalled? (eg: number of twitter followers not updating)
* Did the data format change?
* We need a way to handle know effects (eg: Christmas dip)
* We need to correlate independent datasources
* Be able to validade big chunks of reprocessing
* Do we have clearly wrong rows in resultset? (eg: a `<null>` line there)
* etc

So we decide to build CDV - a data validator that periodically do a set of tests that answer all the above questions.

You can see the [Original RFC for CDV](http://pedroalves-bi.blogspot.com/2012/05/cdv-request-for-comments.html). Note
that not every feature has been implemented


User interface
--------------


The CDV can be called by clicking on the CDV icon on the PUC's toolbar and a new window will open with the following header:

![CDV header](http://www.webdetails.pt/cdv/cdv_header.png)

These are the features in the main user interface (this is the ultimate goal, the implementation may be broken into stages):

* See existing validations 
	* Allow to fire a specific validation 
	* Get the url of a specific validation / all validations 
* Create / Edit validation 
	* Define query name 
	* Define queries and parameters 
	* Define validation function 
	* Chose log alerts (when to throw error / severe / warn / ok) 
	* Choose duration thresholds 
	* Define error message 
	* Define cron 
* Validation status dashboard 
* CDA Query error dashboard
	* Query and parameters 
	* Error 
	* Incidents 
* Duration dashboard to identify slow points in the system 
	* Query and parameters 
	* Duration 
	* Incidents 


Creating New Validations
------------------------


All the validations created will be stored in `solution/cdv/tests/`

The files will have the format `Name.cdv` and will internally be a _JSON_ file with the following structure:

![CDV test](http://www.webdetails.pt/cdv/cdv_editor.png)

Each test has to have an unique id.
You can set diferent data sources by selecting diferent cda files, and set the tests with the following structure:


	validation: [ 
		{ cdaFile: "/solution/cda/test.cda", dataAccessId: "1" , parameters: [...] },
		{ cdaFile: "/solution/cda/test2.cda", dataAccessId: "2" , parameters: [...] }
	],
	tests:[ 
	{
		validationType: "custom",
		validationFunction:  function(rs, conf) {
			var exists = rs.map(function(r){return r.length > 0}).reduce(function(prev, curr){
				return conf.testAll ? (curr && prev) : (curr || prev);
			});
			return exists ? Alarm.ERROR : Alarm.OK;
		}
	}],


We'll be using the Steel-Wheels Sample Data to create some examples of the validations that can be done.

As an example, the following MDX query returns the # of Quantity and Sales for a specific year:

	select NON EMPTY {[Measures].[Quantity], [Measures].[Sales]} ON COLUMNS,
	{Descendants 
		( [Time].[${yearParameter}], 
		[Time].[${yearParameter}], AFTER
		)
	} ON ROWS
	from [SteelWheelsSales]`

For example, for 2003 the resultset is:

	["QTR1", 4561, 445094.69], ["Jan", 1357, 129753.6], ["Feb", 1449, 140836.19000000006], ["Mar", 1755, 174504.89999999997],
	["QTR2", 5695, 564842.02], ["Apr", 1993, 201609.55], ["May", 2017, 192673.11000000002], ["Jun", 1685, 170559.36000000004],
	["QTR3", 6629, 687268.8699999998], ["Jul", 2145, 225486.21000000002], ["Aug", 1974, 197809.30000000002], ["Sep", 2510, 263973.36], 
	["QTR4", 19554, 1980178.4199999995], ["Oct", 5731, 589963.9], ["Nov", 10862, 1086720.4000000001], ["Dec", 2961, 303494.11999999994]

We can test if the resultset has data for all months and quarters as expected, and test the variations between
months and quarters to detect peaks or valleys in the data due to double process or no process.

For that here is an test as example using the CDA file with the MDX query shown above:

		
	cdv.registerTest({
		id: 99999,
		type: "query",
		name: 'Existence of data',
		group: "Steel-Wheels",
		path: 'cdv/tests/steelwheels-existence.cdv',
		createdBy: 'Webdetails',
		createdAt: 1339430893246,
		queries: [ 
		{
			cdaFile: "/plugin-samples/cdv/steelwheels-tests.cda", 
			dataAccessId: "monthlyQuery" , 
			parameters: {   
				yearParameter: "2003"
			}
		}
		],
		validations:[{
			validationName: "Steel-Wheels Data Validation",
			validationType: "custom",
			validationFunction:  function(rs, conf) {
				var success = true,
				dif1 = [], dif2 = [];
				
				//Test existence of data
				var i = rs[0].resultset.length;
				if ( i < 16 ) {
					return {type: "ERROR", description: "Missing data in Steels-Wheels!"};
				}
				
				
				return success ? "OK" : {type: "ERROR", description: "Missing data in Steels-Wheels!"};
			}
		}],
		executionTimeValidation: {
			expected: 100,
			warnPercentage: 0.30,
			errorPercentage: 0.70,
			errorOnLow: false
		},
	 

		cron: "0 0 10 * ? *" 
	});


Since the resultset is supposed to show all quarters and months of a year, we expect that it has 16 rows,
so we can use that to check if we have all the data for the year we want.
As alternative we can also check if there are `<null>` values on the resultset.

If the test fails is shows the messaged settled in the return command: 

`Steel-Wheels: Existence of data and variation - 1 ERROR [Missing data in Steels-Wheels!]`

If the previous test pass then it will do the variations tests. It uses a trigger of lower than 10% for quarter variation
and greater than 200% for monthly variation, but feel free to change it as you will and see the results.

At the bottom, in `executionTimeValidation`, you can set the `expected` time that a query should take to run
and set the `warnPercentage` and `errorPercentage` margin to receive alerts when a certain query takes too long to run.
Also if the query runs too fast you should receive an alerts if you set the `errorOnLow` to true.

For last, you can schedule a time for the test to run automatically on the `cron` line, using the cron predefined scheduling definitions.

If we were using another CDA file or had another `dataAccessId` returning a new resultset, it can be called with `rs[1].resultset`,
where `1` is according to the order settled in the queries section, from top to bottom.

The tests will be sorted by groups, defined when creating each test. In each group, each line corresponds
to a test, where we can see the name of the test, the path to the cda file used as Data Source,
the Validation name, the Expected Duration of the query, the Status of the test and an Options button.

If the test returned a WARN the last time it runned, the font will change to orange, and in case of an ERROR, to red.

### Invocation and Scheduling

There are 2 ways to call the validations:

* By url request 
* Scheduled calls 

Url will be based on the id / query name (tbd). The schedule calls is cron based


Alerts
------

On Alerts you can see the runned tests sorted by time of run and filter them by the Status of the test:


CDA Errors
----------

On CDA Errors there will be a list of any error found on a cda query.

Slow Queries
------------

As for Slow Queries, it shows a list of queries that toke more time to execute than the estimated time set in the cdv file of the test.

Notifications
-------------

There is several external interfaces supported:

* Email
* Http
* Nagios integration
* Server up check

The last one is a very specific check. All the other integrations will fail if suddenly the server hangs, and we must be notified of that. On http and nagios integration, we'll be able to get reports on the individual tests and also on the test groups. This will not rerun the tests but get the report on the last status of a test.

On the http case, we can pass a flat to force a test to be rerun.

For nagios, we can have an export of test rules


Issues, bugs and feature requests
---------------------------------


In order to report bugs, issues or feature requests, please use the [Webdetails CDV Project Page](http://redmine.webdetails.org/projects/cdv/issues)


License
-------

CDG is licensed under the [MPLv2](http://www.mozilla.org/MPL/2.0/) license.
