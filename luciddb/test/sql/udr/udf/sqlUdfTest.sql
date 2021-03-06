-- $Id$
-- Tests for SQL routines

create schema udftest3;
set schema 'udftest3';
set path 'udftest3';


-- Tests for AGE_RANGE

create function age_range(ar varchar(255))
returns integer
language sql
deterministic
contains sql
return 
(case ar 
  when 'Under 15' then 1
  when 'Under 18' then 2
  when '15-17' then 3
  when '19-29' then 4
  when '18-24' then 5
  when '30-39' then 6
  when '25-34' then 7
  when '40-49' then 8
  when '35-44' then 9
  when '50-59' then 10
  when '45-54' then 11
  when '60-69' then 12
  when '55-64' then 13
  when 'Over 70' then 14
  when 'Over 65' then 15
  else 0 
end);

values age_range('30-39');
values age_range('55');


-- Tests for COMPANY_REVENUE

create function company_revenue(rev float)
returns varchar(255)
language sql
contains sql
return (
case  
  WHEN (rev >= 1 and rev <= 499999) THEN '   0 - 0.5 million' 
  WHEN (rev > 499999 and rev <= 999999) THEN '   0.5 - 1 million' 
  WHEN (rev > 999999 and rev <= 4999999) THEN '   1 - 4.99 million' 
  WHEN (rev > 4999999 and rev <= 9999999) THEN '   5 - 9.99 million' 
  WHEN (rev > 9999999 and rev <= 49999999) THEN '  10 - 49.99 million' 
  WHEN (rev > 49999999 and rev <= 99999999) THEN '  50 - 99.99 million' 
  WHEN (rev > 99999999 and rev <= 499999999) THEN ' 100 - 499.99 million' 
  WHEN (rev > 499999999 and rev <= 999999999) THEN ' 500 - 999.99 million' 
  WHEN (rev > 999999999 ) THEN ' > 1 billion' 
  else 'UNKNOWN'
end);

values company_revenue(590234890);
values company_revenue(-20);
values company_revenue(3.32);
values company_revenue(12312.372189);

-- Tests for EDUCATIONAL_LEVEL

create function educational_level(el varchar(60))
returns integer
language sql
contains sql
return (
case el
  when 'In High School' then 1 
  when 'High School' then 2 
  when 'In College' then 3 
  when 'Some College' then 4
  when 'College'  then 5
  when 'College Graduate'  then 6
  when 'In Graduate School' then 7
  when 'Graduate' then 8
  when 'Graduate Degree' then 9
  when 'Professional Degree' then 10
  ELSE 0
END);

values educational_level('In College');
values educational_level('Graduate ');
values educational_level('High School          ');
values educational_level('Some');
values educational_level('College Degree');


-- Tests for NUMBER_OF_EMPLOYEES

create function number_of_employees(noe integer)
returns varchar(255)
language sql
contains sql
return (
case
  WHEN (noe >= 1 and noe <= 24) THEN '    1-24' 
  WHEN (noe >= 25 and noe <= 49) THEN '   25-49' 
  WHEN (noe >= 50 and noe <= 99) THEN '   50-99' 
  WHEN (noe >= 100 and noe <= 499) THEN '  100-499' 
  WHEN (noe >= 500 and noe <= 999) THEN '  500-999' 
  WHEN (noe >= 1000 and noe <= 4999) THEN ' 1000-4999' 
  WHEN (noe >= 5000 and noe <= 9999) THEN ' 5000-5999' 
  WHEN (noe >= 10000 and noe <= 19999) THEN '10000-19999' 
  WHEN (noe >= 20000 ) THEN '> 20000' 
  else 'UNKNOWN'
end);

values number_of_employees(12);
values number_of_employees(123890109);
values number_of_employees(324.324);
values number_of_employees(-234);


-- Tests for INCOME_RANGE
create function income_range(ir varchar(255))
returns integer
language sql
contains sql
return (
case ir
  WHEN '<$14,999' THEN 1 
  WHEN 'Under $35,000' THEN 2
  WHEN  '$15,000-$19,999' THEN 3 
  WHEN  '$35,000 to $49,999' THEN 4
  WHEN  '$20,000-$39,999' THEN 5
  WHEN  '$50,000 to $74,999' THEN 6
  WHEN '$40,000-$59,999' THEN 7
  WHEN '$75,000 to $99,999' THEN 8
  WHEN '$60,000-$75,000' THEN 9
  WHEN '$100,000 to $124,999' THEN 10
  WHEN '>$75,000' then 11
  WHEN '$125,000 and above' then 12
  else 0
end);

values income_range('<$14,99');
values income_range('$125,000');
values income_range('$20,000-$39,999    ');