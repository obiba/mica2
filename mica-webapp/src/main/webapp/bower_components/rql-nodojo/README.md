This is an implementation of Resource Query Language (RQL) that does not use the DOJO AMD framework.  The implementation primarily focuses on the ability to apply an RQL query string against a JSON array.  

The original project (in AMD form) can be found at [github.com/kriszyp/rql](https://github.com/kriszyp/rql), along with some good documentation. It's some good stuff, check it out. 
This [query engine sandbox](http://rql-engine.eu01.aws.af.cm/) is also useful for learning and testing with the original RQL engine.

# Using the Library

Add the script to your project.

	<script src="rql.min.js"></script>

The library adds three objects to the global namespace

	RqlArray
	RqlQuery
	RqlParser
	
For the most part, only RqlArray is needed (the other two classes support the internals of RqlArray, though you can use them for custom trickery).  

A simple example that filters an array for objects where the age property is less than 40:

	var ra = new RqlArray(),
        data = [{name: "Danbo", age: 25}, {name: "Jimbo", age: 45}, {name: "Hambo", age: 9}],
        query = "lt(age,40)",
	    result;
	result = ra.executeQuery(query, {}, data);
	
	//the contents of result would be  [{name: "Danbo", age: 25}, {name: "Hambo", age: 9}]

# Limitations

The conversion testing primarily focused on using string-based functional queries.    E.g.

	"and(eq(name,Jimbo),gt(age,24))"

URL type queries also seem to be working but have not been thoroughly tested.  There are issues with OR and brackets, though these also exist in the original RQL library.  E.g.

	"name=Jimbo&age=gt=24"
	"(name=Jimbo&age>24)|(name=Hambo&age<43)"  //this query usually throws an error

Inline chained queries was not tested and is quite possibly broken due to the removal of the DOJO module code.  E.g.

	var fooBetween3And10Query = new RqlQuery().lt("foo",3).gt("foo",10);
	

String-based functional queries are what will be used by the RAMP project, and is recommended to anyone else using this implementaiton.  Of course, fixes to the other formats are always welcome via a pull request.

# Function List

A quick lookup for function names. Detailed examples of each can be found in the Examples section below

* eq(&lt;property>,&lt;value>) - equal to
* ne(&lt;property>,&lt;value>) - not equal to
* le(&lt;property>,&lt;value>) - less than or equal to
* ge(&lt;property>,&lt;value>) - greater than or equal to
* lt(&lt;property>,&lt;value>) - less than
* gt(&lt;property>,&lt;value>) - greater than
* match(&lt;property>,&lt;regex>) - regular expression match, case insensitive
* matchcase(&lt;property>,&lt;regex>) - regular expression match, case sensitive
* in(&lt;property>,&lt;array-of-values>) - equal to one of a set of values
* out(&lt;property>,&lt;array-of-values>) - not equal to any of a set of values
* contains(&lt;property>,&lt;value | expression>) - array contains a value that satisfies an expression
* excludes(&lt;property>,&lt;value | expression>) - array does not contain a value that satisfies an expression
* between(&lt;property>,&lt;range-of-values>,...) - value is within a range
* and(&lt;query>,&lt;query>,...) - logical and
* or(&lt;query>,&lt;query>,...) - logical or
* sort(&lt;+|->&lt;property,...) - sorts array on property values
* select(&lt;property>,&lt;property>,...) - only includes specific properties in the result set
* unselect(&lt;property>,&lt;property>,...) - exclude specific properties from the result set
* values(&lt;property>,&lt;property>,...) - returns array of specific property values
* limit(count,start) - return a range of the array
* distinct() - return an array of distinct values
* recurse(&lt;property?>) - copy values from child arrays into top array
* sum(&lt;property?>) - sum of values of a property
* mean(&lt;property?>) - mean average of values of a property
* max(&lt;property?>) - maximum value of a property
* min(&lt;property?>) - minimum value of a property
* count() - number of elements in top array
* first() - first element in top array
* one() - first and only element in top array
* aggregate(&lt;property|function>,...) - perform group-by aggregation


# Examples

Here are some fun examples of how to apply query functions in RQL.  Examples will often be in the following format:

	data = [<some objects>];
	query = "<a query string>";
	result = <result from the executeQuery function>;   

The equality after the result variable is just for readability.  In actual code, the result would be set by calling the executeQuery function.

## General Things

Spaces between parameters are treated as values.

	"eq(firstName,Jimbo)" will match against "Jimbo"
	"eq(firstName, Jimbo)" will match against " Jimbo"

If a query requires an array of values as a parameter, the array is constructed with round brackets and comma delimiters.

	"in(colour,(red,purple,orange))"

To drill into nested JSON object properties, supply an array of property names instead of a single property name.

	"eq((car,colour),green)" 
	will inspect the colour property in a data array like 
	[{name: "Jimbo", car: {make: "Dodge", colour: "green"}}]
	
Alternately, arrays can be specified with a slash notation.

	"in(colour,red/purple/orange)"
	"eq(car/colour,green)"
	
To target the objects in the data array instead of a property of them, leave the property value blank.

	"eq(,Jimbo)" is appropriate for a data array like this: ["Danbo", "Jimbo", "Hambo"]

Functions can be chained together with commas. This is particularly useful to apply a function that affects the entire array after you have filtered the array.

	"eq(firstName,Jimbo),sort(+lastName),select(firstName,lastName,age)"
	
For boolean values, use lower case constants to define true or false

	"eq(inSchool,true)"

Primitive data types can be specified in the query by putting the type before the value, followed by a colon.  

	"eq(id,string:5)" will search for the string "5", not the integer 5

## Functions That Filter

### eq

Filters objects where the value of the given property is equal to the given value.  
**Usage:** eq(&lt;property>,&lt;value>) 
	
	data = [
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 44}
	];
	query = "eq(name,Jimbo)";
	result = [		
		{name: "Jimbo", age: 37},
		{name: "Jimbo", age: 44}
	];
	
Alternate forms

	"name=Jimbo"
	"name==Jimbo"
	"name=eq=Jimbo"


### ne

Filters objects where the value of the given property is not equal to the given value.  
**Usage:** ne(&lt;property>,&lt;value>) 
	
	data = [
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 44}
	];
	query = "ne(name,Jimbo)";
	result = [		
		{name: "Danbo", age: 25},		
		{name: "Hambo", age: 40}
	];
	
Alternate forms

	"name!=Jimbo"
	"name=ne=Jimbo"
	
### le

Filters objects where the value of the given property is less than or equal to the given value.  
**Usage:** le(&lt;property>,&lt;value>) 
	
	data = [
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 44}
	];
	query = "le(age,40)";
	result = [		
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40}
	];
	

	query = "le(name,I)";
	result = [		
		{name: "Danbo", age: 25},		
		{name: "Hambo", age: 40}
	];
	
	
Alternate forms

	"age<=40"
	"age=le=40"


### ge

Filters objects where the value of the given property is greater than or equal to the given value.  
**Usage:** ge(&lt;property>,&lt;value>) 
	
	data = [
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 44}
	];
	query = "ge(age,40)";
	result = [		
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 44}
	];
	

	query = "ge(name,J)";
	result = [		
		{name: "Jimbo", age: 37},
		{name: "Jimbo", age: 44}
	];
	
	
Alternate forms

	"age>=25"
	"age=ge=25"


### lt

Filters objects where the value of the given property is less than the given value.  
**Usage:** lt(&lt;property>,&lt;value>) 
	
	data = [
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 44}
	];
	query = "lt(age,40)";
	result = [		
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37}
	];
	

	query = "lt(name,J)";
	result = [		
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40}
	];
		
	
Alternate forms

	"age<35"
	"age=lt=35"


### gt

Filters objects where the value of the given property is greater than the given value.  
**Usage:** gt(&lt;property>,&lt;value>) 
	
	data = [
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 44}
	];
	query = "gt(age,40)";
	result = [		
		{name: "Jimbo", age: 44}
	];
	

	query = "gt(name,J)";
	result = [		
		{name: "Jimbo", age: 37},
		{name: "Jimbo", age: 44}
	];	
	
Alternate forms

	"age>42"
	"age=gt=42"


### match

Filters objects where the value of the given property satisfies the given regex pattern, with case insensitivity turned on.  
**Usage:** match(&lt;property>,&lt;regex>) 

	data = [
		{name: "danbo", age: 25},
		{name: "Mc ron dougal", age: 37},
		{name: "Hambo", age: 40},
		{name: "Mc Daniel", age: 44}
	];
	query = "match(name,da)";
	result = [		
		{name: "danbo", age: 25},				
		{name: "Mc Daniel", age: 44}
	];
	

	query = "match(name,mc*d)";
	result = [		
		{name: "Mc ron dougal", age: 37},	
		{name: "Mc Daniel", age: 44}
	];	


### matchcase

Filters objects where the value of the given property satisfies the given regex pattern, with case sensitivity turned on.  
**Usage:** matchcase(&lt;property>,&lt;regex>) 

	data = [
		{name: "danbo", age: 25},
		{name: "Mc ron dougal", age: 37},
		{name: "Hambo", age: 40},
		{name: "Mc Daniel", age: 44}
	];
	query = "matchcase(name,da)";
	result = [							
		{name: "danbo", age: 25}
	];
	

	query = "matchcase(name,Mc*D)";
	result = [					
		{name: "Mc Daniel", age: 44}
	];	


### in

Filters objects where the value of the given property is equal to one of the values in the given array of values.  
**Usage:** in(&lt;property>,&lt;array-of-values>)

	data = [
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40},
		{name: "Daniel", age: 44}
	];
	query = "in(name,(Dan,Daniel,Danbo))";
	result = [							
		{name: "Danbo", age: 25},
		{name: "Daniel", age: 44}
	];
	


### out

Filters objects where the value of the given property is not equal to any of the values in the given array of values.  
**Usage:** out(&lt;property>,&lt;array-of-values>)

	data = [
		{name: "Danbo", age: 25},
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40},
		{name: "Daniel", age: 44}
	];
	query = "out(name,(Dan,Daniel,Danbo))";
	result = [							
		{name: "Jimbo", age: 37},
		{name: "Hambo", age: 40}
	];
	

### contains

Filters objects where the value of the given property is an array, and that array has an element that equals the given value or satisfies the given expression.  
**Usage:** contains(&lt;property>,&lt;value | expression>) 

	//using value match
	data = [
		{
			order: "00235",
			toppings: ["mushroom", "green pepper", "bacon"]
		},
		{
			order: "00236",
			toppings: ["mushroom", "tomato", "onion"]
		}
	];
	query = "contains(toppings,onion)";
	result = [		
		{
			order: "00236",
			toppings: ["mushroom", "tomato", "onion"]
		}
	];


	//using expression match
	data = [
		{
			order: "00235",
			toppings: [
				{name: "mushroom", quantity: 1},
				{name: "green pepper", quantity: 1},
				{name: "bacon",	quantity: 2}	
			]
		},
		{
			order: "00236",
			toppings: [
				{name: "mushroom", quantity: 1},
				{name: "tomato", quantity: 1},
				{name: "onion",	quantity: 0.5}	
			]
		}
	];
	query = "contains(toppings,eq(name,bacon))";
	result = [		
		{
			order: "00235",
			toppings: [
				{name: "mushroom", quantity: 1},
				{name: "green pepper", quantity: 1},
				{name: "bacon",	quantity: 2}	
			]
		}
	];

### excludes

Filters objects where the value of the given property is an array, and that array has no elements that equal the given value or satisfies the given expression.  
**Usage:** excludes(&lt;property>,&lt;value | expression>) 

	//using value match
	data = [
		{
			order: "00235",
			toppings: ["mushroom", "green pepper", "bacon"]
		},
		{
			order: "00236",
			toppings: ["mushroom", "tomato", "onion"]
		}
	];
	query = "excludes(toppings,bacon)";
	result = [		
		{
			order: "00236",
			toppings: ["mushroom", "tomato", "onion"]
		}
	];


	//using expression match
	data = [
		{
			order: "00235",
			toppings: [
				{name: "mushroom", quantity: 1},
				{name: "green pepper", quantity: 1},
				{name: "bacon",	quantity: 2}	
			]
		},
		{
			order: "00236",
			toppings: [
				{name: "mushroom", quantity: 1},
				{name: "tomato", quantity: 1},
				{name: "onion",	quantity: 0.5}	
			]
		}
	];
	query = "excludes(toppings,eq(name,onion))";
	result = [		
		{
			order: "00235",
			toppings: [
				{name: "mushroom", quantity: 1},
				{name: "green pepper", quantity: 1},
				{name: "bacon",	quantity: 2}	
			]
		}
	];


### between

Filters objects where the value of the given property is within the given range. The range is inclusive on the lower bound, and exclusive on the upper bound.  
**Usage:** between(&lt;property>,&lt;range-of-values>,...)


	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 44},
		{name: "Aunt Punchy", age: 37},
		{name: "Uncle Shouty", age: 17}
	];
	query = "between(age,(25,44))";
	result = [		
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},	
		{name: "Aunt Punchy", age: 37}
	];
	
	
	query = "between(name,(H,M))";
	result = [				
		{name: "Hambo", age: 40},	
		{name: "Jimbo", age: 44}
	];
	

### and

Performs a logical AND on two or more queries.  
**Usage:** and(&lt;query>,&lt;query>,...) 

	data = [
		{name: "Danbo", age: 25, salary: 23000, gender: "male"},
		{name: "Hambo", age: 40, salary: 35000, gender: "female"},
		{name: "Jimbo", age: 40, salary: 38000, gender: "male"},
		{name: "Jenbo", age: 22, salary: 21000, gender: "female"},
		{name: "Aunt Punchy", age: 40, salary: 52000, gender: "female"}
	];
	query = "and(eq(gender,female),lt(salary,50000),match(name,j))";
	result = [				
		{name: "Jenbo", age: 22, salary: 21000, gender: "female"}
	];

Alternate form
	
	"eq(gender,female)&lt(salary,50000)&match(name,j)"

### or

Performs a logical OR on two or more queries.  
**Usage:** or(&lt;query>,&lt;query>,...) 

	data = [
		{name: "Danbo", age: 25, salary: 23000, gender: "male"},
		{name: "Hambo", age: 30, salary: 35000, gender: "female"},
		{name: "Jimbo", age: 40, salary: 38000, gender: "male"},
		{name: "Jenbo", age: 22, salary: 21000, gender: "female"},
		{name: "Aunt Punchy", age: 40, salary: 52000, gender: "female"}
	];
	query = "or(eq(age,40),gt(salary,50000),match(name,h))";
	result = [						
		{name: "Hambo", age: 30, salary: 35000, gender: "female"},
		{name: "Jimbo", age: 40, salary: 38000, gender: "male"},		
		{name: "Aunt Punchy", age: 40, salary: 52000, gender: "female"}
	];

Alternate form (gets cranky when brackets are introduced to the query string)

	"firstName=John|firstName=Johnny|firstName=Jon"


## Functions That Change The Result Array

### sort

Will sort the array based on the given properties. The leading + or - dictates the order of the sort.  
**Usage:** sort(&lt;+|->&lt;property,...)

	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 17}
	];
	query = "lt(age,30),sort(-name)";
	result = [		
		{name: "Jimbo", age: 17},
		{name: "Danbo", age: 25}
	];


	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 40}
	];
	query = "sort(-age,+name)";
	result = [				
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 40},
		{name: "Danbo", age: 25}
	];


### select

Will return the array objects containing only the given properties.  
**Usage:** select(&lt;property>,&lt;property>,...) 

	data = [
		{name: "Danbo", age: 25, mood: "feisty"},
		{name: "Hambo", age: 40, mood: "serene"},
		{name: "Jimbo", age: 17, mood: "hyper"}
	];
	query = "lt(age,30),select(name,mood)";
	result = [		
		{name: "Danbo", mood: "feisty"},		
		{name: "Jimbo", mood: "hyper"}
	];

### unselect

Will return the array objects with the given properties removed.  
**Usage:** unselect(&lt;property>,&lt;property>,...) 

	data = [
		{name: "Danbo", age: 25, mood: "feisty"},
		{name: "Hambo", age: 40, mood: "serene"},
		{name: "Jimbo", age: 17, mood: "hyper"}
	];
	query = "lt(age,30),unselect(age)";
	result = [		
		{name: "Danbo", mood: "feisty"},		
		{name: "Jimbo", mood: "hyper"}
	];

### values

Will return an array of values for a given property.  If multiple properties are given, will return nested arrays of values for each data item.  
**Usage:** values(&lt;property>,&lt;property>,...)

	//single property
	data = [
		{name: "Danbo", age: 25, mood: "feisty"},
		{name: "Hambo", age: 40, mood: "serene"},
		{name: "Jimbo", age: 17, mood: "hyper"}
	];
	query = "lt(age,30),values(name)";
	result = ["Danbo", "Jimbo"];


	//multiple properties
	data = [
		{name: "Danbo", age: 25, mood: "feisty"},
		{name: "Hambo", age: 40, mood: "serene"},
		{name: "Jimbo", age: 17, mood: "hyper"}
	];
	query = "lt(age,30),values(name,mood)";
	result = [
		["Danbo", "feisty"],		
		["Jimbo", "hyper"]
	];


### limit

Will return a range of the data array.  First parameter is the number of items in the range. Second parameter is zero-based index where the range begins.  
**Usage:** limit(count,start)

	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 44},
		{name: "Aunt Punchy", age: 37},
		{name: "Uncle Shouty", age: 17}
	];
	query = "le(age,40),limit(2,1)";
	result = [		
		{name: "Hambo", age: 40},
		{name: "Aunt Punchy", age: 37}
	];

The source code and original doc has a third parameter, maxCount. If supplied, it appears adds extra properties to the intermediate result (.start, .end, .totalCount), but nothing seems to use those values for anything.


### distinct

Will return a list of unique values from the data array. Works only on simple value types.  Identical objects will be treated as different.  
**Usage:** distinct()

	//against values
	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Hambo", age: 44}
	];
	query = "values(name),distinct()";
	result = [		
		"Danbo",
		"Hambo"
	];


	//disappointing object comparison
	data = [		
		{name: "Hambo", age: 40},
		{name: "Hambo", age: 40}
	];
	query = "distinct()";
	result = [		
		{name: "Hambo", age: 40},
		{name: "Hambo", age: 40}
	];


### recurse

Will take any arrays that belong to the given property and insert those array values into the main array.  Will recurse into nested arrays.  
**Usage:** recurse(&lt;property?>)

	//property example
	data = [		
		{
			name: "Jimbo", 
		 	orders: [{id: 25}, {id: 40}]
		},
		{
			name: "Danbo",
			orders: [{id: 19}]
		}
	];
	query = "recurse(orders)";
	result = [		
		{
			name: "Jimbo", 
		 	orders: [{id: 25}, {id: 40}]
		},
		{id: 25}, 
		{id: 40},
		{
			name: "Danbo",
			orders: [{id: 19}]
		},
		{id: 19}
	];


	//top level example with nested arrays
	data = [		
		[1, 2, 3],
		[
			[4, 5],
			[6, 7]
		]
	];
	query = "recurse()";
	result = [1, 2, 3, 4, 5, 6, 7];
	

## Functions That Aggregate The Result Array

### sum

Returns the sum of the values in the data array. If a property is provided, sums on the value of the property.  Values must be numeric.  
**Usage:** sum(&lt;property?>)

	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 17}
	];
	query = "sum(age)";
	result = 82;


	data = [5, 8, 13, 21];
	query = "sum()";
	result = 47;


### mean

Causes RQL to get angry.  Just kidding, y'all.  Returns the mean average of the values in the data array. If a property is provided, averages on the value of the property.  Values must be numeric.  
**Usage:** mean(&lt;property?>)

	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 17}
	];
	query = "mean(age)";
	result = 27.33333333333333;


	data = [5, 8, 13, 21];
	query = "mean()";
	result = 11.75;


### max

Returns the maximum of the values in the data array. If a property is provided, returns the maximum value of the property.  Values must be numeric.  
**Usage:** max(&lt;property?>)

	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 17}
	];
	query = "max(age)";
	result = 40;


	data = [5, 8, 13, 21];
	query = "max()";
	result = 21;
	
	
### min

Returns the minimum of the values in the data array. If a property is provided, returns the minimum value of the property.  Values must be numeric.  
**Usage:** min(&lt;property?>)

	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 17}
	];
	query = "min(age)";
	result = 17;


	data = [5, 8, 13, 21];
	query = "min()";
	result = 5;


### count

Returns the number of values in the data array.  
**Usage:** count()

	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 17}
	];
	query = "count()";
	result = 3;

	query = "lt(age,40),count()";
	result = 2;


### first

Returns the first value in the data array.  
**Usage:** first()

	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 37}
	];
	query = "gt(age,30),first()";
	result = {name: "Hambo", age: 40};


### one

Returns the first and only value in the data array. If array has more than one value, an error is thrown.  
**Usage:** one()

	data = [
		{name: "Danbo", age: 25},
		{name: "Hambo", age: 40},
		{name: "Jimbo", age: 17}
	];
	query = "gt(age,30),first()";
	result = {name: "Hambo", age: 40};

	query = "gt(age,20),first()";
	result = ERROR!!!!;


### aggregate

Returns the result of an aggregation on the dataset. Allows you to group by values, and apply aggregate functions on the data in those groups.  Takes an arbitrary list of properties and aggregate functions. Will return an array of objects for each grouping.  Each object will contain the specific values of the grouping, and the result of the aggregate functions for that grouping.  Function results are stored in properties with integer numbers as names, corresponding to the order the functions were supplied in the query.  
**Usage:** aggregate(&lt;property|function>,...)

	data = [
		{name: "Danbo", age: 25, salary: 23000, gender: "male"},
		{name: "Hambo", age: 40, salary: 35000, gender: "female"},
		{name: "Jimbo", age: 40, salary: 38000, gender: "male"},
		{name: "Aunt Punchy", age: 40, salary: 52000, gender: "female"}
	];
	
	//average salary grouped by age
	query = "aggregate(age,mean(salary))";
	result = [
		{age: 25, "0": 23000}, 
		{age: 40, "0": 41666.6666666}
	];

	//average salary grouped by age and gender, with count of group membership
	query = "aggregate(age,gender,mean(salary),count())";
	result = [
		{age: 25, gender: "male", "0": 23000, "1": 1}, 
		{age: 40, gender: "female", "0": 43500, "1": 2},
		{age: 40, gender: "male", "0": 38000, "1": 1}
	];

	//maximum salary of people older than 30, grouped by gender
	query = "gt(age,30),aggregate(gender,max(salary))";
	result = [
		{gender: "male", "0": 38000}, 
		{gender: "female", "0": 52000}
	];


## Array Query Options

The middle parameter of RqlArray().executeQuery(,,) is an options object.  Usually it is just an empty object.  However, there are two properties that may be added for additional functionalities.

### Operators

The operators property is set to an object with additional functions defined on it.  These functions are injected into the RQL function engine and can be used in a query. 

	var ra = new RqlArray(),
        data = [{name: "Danbo", age: 25}, {name: "Jimbo", age: 4}, {name: "Hambo", age: 31}],
        query = "last()",
		options = {
            operators: {
                last: function () {
                    return this[this.length -1];
                }  
            }
        };
	    result;
	result = ra.executeQuery(query, options, data);
	
	//the contents of result would be {name: "Hambo", age: 31}


### Parameters

The parameters property is set to an array of values.  These values will be substituted into any $# placeholders in the query string.

	var ra = new RqlArray(),
        data = [{name: "Danbo", age: 25}, {name: "Jimbo", age: 45}, {name: "Hambo", age: 3}],
        query = "or(eq(name,$1),gt(age,$2))",
		options = {
            parameters: ["Danbo", 36]            
        };
	    result;
	result = ra.executeQuery(query, options, data);
	
	//the contents of result would be [{name: "Danbo", age: 25}, {name: "Jimbo", age: 45}]


Licensing
--------

TODO review by aly

This code was "borrowed" from RQL, which is licensed under the AFL or BSD license as part of the [Persevere](https://github.com/persvr) project.



