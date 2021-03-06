-- $Id$
-- This script creates a view schema used for database management

!set verbose true

-- create views in system-owned schema sys_boot.mgmt
create or replace schema sys_boot.mgmt;
set schema 'sys_boot.mgmt';
set path 'sys_boot.mgmt';

create or replace function repository_properties()
returns table(property_name varchar(255), property_value varchar(255))
language java
parameter style system defined java
no sql
external name 'class net.sf.farrago.syslib.FarragoManagementUDR.repositoryProperties';

create or replace view repository_properties_view as
  select * from table(repository_properties());

create or replace function repository_integrity_violations()
returns table(description varchar(65535), mof_id varchar(128))
language java
parameter style system defined java
no sql
external name 'class net.sf.farrago.syslib.FarragoManagementUDR.repositoryIntegrityViolations';

create or replace function statements()
returns table(id bigint, session_id bigint, sql_stmt varchar(1024), create_time timestamp, parameters varchar(1024))
language java
parameter style system defined java
no sql
external name 'class net.sf.farrago.syslib.FarragoManagementUDR.statements';

create or replace view statements_view as
  select * from table(statements());

create or replace function sessions()
returns table(id int, url varchar(128), current_user_name varchar(128), current_role_name varchar(128), session_user_name varchar(128), system_user_name varchar(128), system_user_fullname varchar(128), session_name varchar(128), program_name varchar(128), process_id int, catalog_name varchar(128), schema_name varchar(128), is_closed boolean, is_auto_commit boolean, is_txn_in_progress boolean, label_name varchar(128))
language java
parameter style system defined java
no sql
external name 'class net.sf.farrago.syslib.FarragoManagementUDR.sessions';

create or replace view sessions_view as
  select * from table(sessions());

create or replace function objects_in_use()
returns table(session_id bigint, stmt_id bigint, mof_id varchar(128))
language java
parameter style system defined java
no sql
external name 'class net.sf.farrago.syslib.FarragoManagementUDR.objectsInUse';

create or replace view objects_in_use_view as
  select * from table(objects_in_use());

create or replace function threads()
returns table(
    thread_id bigint, thread_group_name varchar(128), thread_name varchar(128),
    thread_priority int, thread_state varchar(128), is_alive boolean,
    is_daemon boolean, is_interrupted boolean)
language java
parameter style system defined java
no sql
external name 'class net.sf.farrago.syslib.FarragoManagementUDR.threadList';

create or replace function thread_stack_entries()
returns table(
    thread_id bigint, stack_level int, entry_string varchar(1024),
    class_name varchar(128), method_name varchar(128),
    file_name varchar(1024), line_num int, is_native boolean)
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoManagementUDR.threadStackEntries';

create or replace function system_info()
returns table(
    category varchar(128),
    subcategory varchar(128),
    source_name varchar(128), 
    item_name varchar(1024), 
    item_units varchar(128),
    item_value varchar(65535))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoManagementUDR.systemInfo';

create or replace function performance_counters()
returns table(
    counter_category varchar(128),
    counter_subcategory varchar(128),
    source_name varchar(128), 
    counter_name varchar(1024), 
    counter_units varchar(128),
    counter_value varchar(1024))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoManagementUDR.performanceCounters';

-- lie and say this is non-deterministic, since it's usually used
-- in cases where it would be annoying if it got optimized away
create or replace function sleep(millis bigint)
returns integer
language java
no sql
not deterministic
external name 'class net.sf.farrago.syslib.FarragoManagementUDR.sleep';

-- flushes all entries from the global code cache
create or replace procedure flush_code_cache()
  language java
  parameter style java
  reads sql data
  external name
  'class net.sf.farrago.syslib.FarragoManagementUDR.flushCodeCache';

-- lets an administrator kill a running session
create or replace procedure kill_session(in id bigint)
  language java
  parameter style java
  no sql
  external name 'class net.sf.farrago.syslib.FarragoKillUDR.killSession';

create or replace procedure kill_session(in id bigint, in cancel_only boolean)
  language java
  parameter style java
  no sql
  specific kill_session_cancel
  external name 'class net.sf.farrago.syslib.FarragoKillUDR.killSession';

-- lets an administrator kill an executing statement
-- (like unix "kill -KILL")
-- param ID: globally-unique statement id
create or replace procedure kill_statement(in id bigint)
  language java
  parameter style java
  no sql
  external name 'class net.sf.farrago.syslib.FarragoKillUDR.killStatement';

create or replace procedure kill_statement(in id bigint, in cancel_only boolean)
  language java
  parameter style java
  no sql
  specific kill_statement_cancel
  external name 'class net.sf.farrago.syslib.FarragoKillUDR.killStatement';

-- kills all statements with SQL matching a given string
-- (like unix pkill)
-- Works around lack of scalar subqueries, which makes kill_statement(id) hard to use
-- param SQL: a string
create or replace procedure kill_statement_match(in s varchar(256))
  language java
  parameter style java
  no sql
  external name 'class net.sf.farrago.syslib.FarragoKillUDR.killStatementMatch';

create or replace procedure kill_statement_match(
    in s varchar(256), in cancel_only boolean )
  language java
  parameter style java
  no sql
  specific kill_statement_match_cancel
  external name 'class net.sf.farrago.syslib.FarragoKillUDR.killStatementMatch';

create or replace procedure shutdown_database(in kill_sessions boolean, in jvm_shutdown_delay_millis bigint)
  language java
  parameter style java
  no sql
  external name 'class net.sf.farrago.syslib.FarragoManagementUDR.shutdownDatabase';

-- sets a filter on the optimizer rules to be used in the current session
create or replace procedure set_opt_rule_desc_exclusion_filter(
    in regex varchar(2000))
language java
contains sql
external name
'class net.sf.farrago.syslib.FarragoManagementUDR.setOptRuleDescExclusionFilter';

-- exports the catalog to an XMI file
create or replace procedure export_catalog_xmi(in filename varchar(65535))
  language java
  parameter style java
  no sql
  external name 'class net.sf.farrago.syslib.FarragoManagementUDR.exportCatalog';

-- exports query results to a delimited file (optionally with BCP control file)
create or replace procedure export_query_to_file(
  in query_sql varchar(65535),
  in path_without_extension varchar(65535),
  in bcp boolean,
  in include_data boolean,
  in delete_failed_file boolean,
  in field_delimiter varchar(2),
  in file_extension varchar(5),
  in date_format varchar(128),
  in time_format varchar(128),
  in timestamp_format varchar(128))
language java
reads sql data
specific export_query_to_file_3
called on null input
external name 'class net.sf.farrago.syslib.FarragoExportSchemaUDR.exportQueryToFile';

-- exports tables in a schema to a delimited file
create or replace procedure export_schema_to_file(
  in cat varchar(128),
  in schma varchar(128),
  in exclude boolean,
  in tlist varchar(65535),
  in tpattern varchar(65535),
  in dir varchar(65535),
  in bcp boolean,
  in delete_failed_file boolean,
  in field_delimiter varchar(2),
  in file_extension varchar(5),
  in date_format varchar(50),
  in time_format varchar(50),
  in timestamp_format varchar(50))
language java
reads sql data
called on null input
external name 'class net.sf.farrago.syslib.FarragoExportSchemaUDR.exportSchemaToFile';

-- switches default character set to Unicode
create or replace procedure change_default_character_set_to_unicode()
language java
no sql
external name 'class net.sf.farrago.syslib.FarragoManagementUDR.setUnicodeAsDefault';

-- Returns session parameters
create or replace function session_parameters ()
returns table(
  param_name varchar(128),
  param_value varchar(128))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoManagementUDR.sessionParameters';

create or replace view session_parameters_view as
  select * from table(session_parameters());

grant select on session_parameters_view to public;

--
-- Statistics
--

-- Get the row count of a table
create or replace function stat_get_row_count(
    catalog_name varchar(2000),
    schema_name varchar(2000),
    table_name varchar(2000))
returns bigint
language java
contains sql
external name 'class net.sf.farrago.syslib.FarragoStatsUDR.get_row_count';

-- Set the row count of a table
create or replace procedure stat_set_row_count(
    in catalog_name varchar(2000),
    in schema_name varchar(2000),
    in table_name varchar(2000),
    in row_count bigint)
language java
contains sql
external name 'class net.sf.farrago.syslib.FarragoStatsUDR.set_row_count';

-- Set the page count of an index
create or replace procedure stat_set_page_count(
    in catalog_name varchar(2000),
    in schema_name varchar(2000),
    in index_name varchar(2000),
    in page_count bigint)
language java
contains sql
external name 'class net.sf.farrago.syslib.FarragoStatsUDR.set_page_count';

-- Generate a histogram for a column
--
-- distribution_type must be 0 for now
-- value_digits are characters to use for fake column values
create or replace procedure stat_set_column_histogram(
    in catalog_name varchar(2000),
    in schema_name varchar(2000),
    in table_name varchar(2000),
    in column_name varchar(2000),
    in distict_values bigint,
    in sample_percent int,
    in sample_distinct_values bigint,
    in distribution_type int,
    in value_digits varchar(2000))
language java
contains sql
external name 'class net.sf.farrago.syslib.FarragoStatsUDR.set_column_histogram';

-- Get cardinality and selectivity for an expression.
--
-- example expressions for an integer column (other types work as well, but
-- note that there's no way to escape brackets or commas in this trivial
-- implementation):
--   '123'      = 123
--   '[123'     >= 123
--   '(123'     > 123
--   '123]'     <= 123
--   '[10,20)'  >= 10 and < 20
create or replace function stat_get_cardinality(
    catalog_name varchar(2000),
    schema_name varchar(2000),
    table_name varchar(2000),
    column_name varchar(2000),
    expression varchar(2000))
returns double
language java
no sql
external name 'class net.sf.farrago.syslib.FarragoStatsUDR.get_cardinality';

create or replace function stat_get_selectivity(
    catalog_name varchar(2000),
    schema_name varchar(2000),
    table_name varchar(2000),
    column_name varchar(2000),
    expression varchar(2000))
returns double
language java
no sql
external name 'class net.sf.farrago.syslib.FarragoStatsUDR.get_selectivity';

-- Statistics views
create or replace view page_counts_view as
    select
        i.table_cat,
        i.table_schem,
        i.table_name,
        i.index_name,
        i.pages
    from
        sys_boot.jdbc_metadata.index_info_internal i
    where
        i.pages is not null
;

create or replace view row_counts_view as
    select
        t.table_cat,
        t.table_schem,
        t.table_name,
        acs."rowCount" as row_count
    from
        sys_boot.jdbc_metadata.tables_view_internal t
    inner join
        sys_fem."SQL2003"."AbstractColumnSet" acs
    on
        t."mofId" = acs."mofId"
    where
        acs."rowCount" is not null
;

create or replace view histograms_view_internal as
    select
        c.table_cat,
        c.table_schem,
        c.table_name,
        c.column_name,
        h."distinctValueCount" as "CARDINALITY",
        h."distinctValueCountEstimated" as cardinality_estimated,
        h."percentageSampled" as percent_sampled,
        h."sampleSize" as sample_size,
        h."barCount" as bar_count,
        h."rowsPerBar" as rows_per_bar,
        h."rowsLastBar" as rows_last_bar,
        cast(h."analyzeTime" as timestamp) as last_analyze_time,
        h."mofId"
    from
        sys_boot.jdbc_metadata.columns_view_internal c
    inner join
        sys_fem.med."ColumnHistogram" h
    on
        c."mofId" = h."Column"
    where
        h."analyzeTime" =
            (select max("analyzeTime") from sys_fem.med."ColumnHistogram"
                where "Column" = h."Column")
;

create or replace view histograms_view as
    select
        h.table_cat,
        h.table_schem,
        h.table_name,
        h.column_name,
        h."CARDINALITY",
        h.cardinality_estimated,
        h.percent_sampled,
        h.sample_size,
        h.bar_count,
        h.rows_per_bar,
        h.rows_last_bar,
        h.last_analyze_time
    from
        histograms_view_internal h
;

create or replace view histogram_bars_view as
    select
        h.table_cat,
        h.table_schem,
        h.table_name,
        h.column_name,
        b."ordinal" as ordinal,
        b."startingValue" as start_value,
        b."valueCount" as value_count
    from
        histograms_view_internal h
    inner join
        sys_fem.med."ColumnHistogramBar" b
    on
        h."mofId" = b."Histogram"
;

--
-- Sequences
--

create or replace view sequences_view as
    select
        c.table_cat,
        c.table_schem,
        c.table_name,
        c.column_name,
        s."baseValue",
        s."increment",
        s."minValue",
        s."maxValue",
        s."cycle",
        s."expired"
    from
        sys_boot.jdbc_metadata.columns_view_internal c
    inner join
        sys_fem."SQL2003"."SequenceGenerator" s
    on
        c."mofId" = s."Column"
;

--
-- Indexes
--

-- TODO:  "creator" should never have been here; eliminate it
-- in a release after LucidDB 0.9.4
create or replace view dba_indexes_internal as
    select
        cast(t.table_cat as varchar(128)) as catalog_name,
        cast(t.table_schem as varchar(128)) as schema_name,
        cast(i."name" as varchar(128)) as index_name,
        cast(null as varchar(128)) as creator,
        cast(i."creationTimestamp" as timestamp) as creation_timestamp,
        cast(i."modificationTimestamp" as timestamp) as modification_timestamp,
        cast(t.table_name as varchar(128)) as table_name,
        i."isUnique" as is_unique,
        i."isClustered" as is_clustered,
        cast(i."description" as varchar(65535)) as remarks,
        i."mofId" as mof_id,
        i."lineageId" as lineage_id
    from
        sys_boot.jdbc_metadata.tables_view_internal t
    inner join
        sys_fem."MED"."LocalIndex" i
    on
        t."mofId" = i."spannedClass"
;

create or replace view dba_index_columns_internal as
    select
        i."mofId" as mof_id,
        c."name" as column_name,
        c."ordinal" as ordinal_position
    from
        sys_boot.jdbc_metadata.index_info_internal i
    inner join
        sys_fem."MED"."LocalIndexColumn" c
    on
        i."mofId" = c."index"
;

--
-- Database admin internal views and functions
--

create or replace function get_table_type_by_mof_class_name(
    mofclassname varchar(128))
returns varchar(128)
contains sql
deterministic
return case
  when mofclassname='LocalView' then 'LOCAL VIEW'
  when mofclassname='LocalTable' then 'LOCAL TABLE'
  when mofclassname='ForeignTable' then 'FOREIGN TABLE'
  else cast(mofclassname as varchar(128))
end;

create or replace view dba_schemas_internal1 as
  select
    cast(c."name" as varchar(128)) as catalog_name,
    cast(s."name" as varchar(128)) as schema_name,
    cast(s."creationTimestamp" as timestamp) as creation_timestamp,
    cast(s."modificationTimestamp" as timestamp) as last_modified_timestamp,
    cast(s."description" as varchar(65535)) as remarks,
    s."mofId",
    s."lineageId"
  from
    sys_fem."SQL2003"."LocalCatalog" c
  inner join
    sys_fem."SQL2003"."LocalSchema" s
  on
    c."mofId" = s."namespace"
;

create or replace view dba_schemas_internal2 as
  select
    catalog_name,
    schema_name,
    creation_timestamp,
    last_modified_timestamp,
    remarks,
    si."mofId",
    si."lineageId",
    g."Grantee"
  from
    dba_schemas_internal1 si
  inner join
    sys_fem."Security"."Grant" g
  on
   si."mofId" = g."Element"
  where
   g."action" = 'CREATION'
;

create or replace view dba_tables_internal1 as
  select
    cast(table_cat as varchar(128)) as catalog_name,
    cast(table_schem as varchar(128)) as schema_name,
    cast(table_name as varchar(128)) as table_name,
    sys_boot.mgmt.get_table_type_by_mof_class_name(t."mofClassName")
        as table_type,
    cast(ae."creationTimestamp" as timestamp) as creation_timestamp,
    cast(ae."modificationTimestamp" as timestamp)
        as last_modification_timestamp,
    cast("description" as varchar(128)) as remarks,
    ae."mofId",
    ae."lineageId"
  from
    sys_boot.jdbc_metadata.tables_view_internal t
  inner join
    sys_fem."SQL2003"."AnnotatedElement" ae
  on
    t."mofId" = ae."mofId"
;

create or replace view dba_tables_internal2 as
  select
    catalog_name,
    schema_name,
    table_name,
    table_type,
    creation_timestamp,
    last_modification_timestamp,
    remarks,
    g."Grantee",
    dti."mofId",
    dti."lineageId"
  from
    dba_tables_internal1 dti
  inner join
    sys_fem."Security"."Grant" g
  on
    dti."mofId" = g."Element"
  where
    g."action" = 'CREATION'
;

create or replace view dba_views_internal1 as
  select
    cast(object_catalog as varchar(128)) as catalog_name,
    cast(object_schema as varchar(128)) as schema_name,
    cast(v."name" as varchar(128)) as view_name,
    cast("creationTimestamp" as timestamp) as creation_timestamp,
    cast("modificationTimestamp" as timestamp) as last_modification_timestamp,
    cast("originalDefinition" as varchar(65535)) as original_text,
    cast("description" as varchar(65535)) as remarks,
    v."mofId",
    v."lineageId"
  from
    sys_boot.jdbc_metadata.schemas_view_internal s
  inner join
    sys_fem."SQL2003"."LocalView" v
  on
    s."mofId" = v."namespace"
;


create or replace view dba_views_internal2 as
  select
    catalog_name,
    schema_name,
    view_name,
    creation_timestamp,
    last_modification_timestamp,
    original_text,
    remarks,
    vi."mofId",
    vi."lineageId",
    g."Grantee"
  from
    dba_views_internal1 vi
  inner join
    sys_fem."Security"."Grant" g
  on
    vi."mofId" = g."Element"
  where
    g."action" = 'CREATION'
;

create or replace view dba_stored_tables_internal1 as
  select
    cast(object_catalog as varchar(128)) as catalog_name,
    cast(object_schema as varchar(128)) as schema_name,
    cast(t."name" as varchar(128)) as table_name,
    cast(t."creationTimestamp" as timestamp) as creation_timestamp,
    cast(t."modificationTimestamp" as timestamp)
        as last_modification_timestamp,
    t."lastAnalyzeRowCount" as last_analyze_row_count,
    cast(t."analyzeTime" as timestamp) as last_analyze_timestamp,
    t."rowCount" as current_row_count,
    t."deletedRowCount" as deleted_row_count,
    cast(t."description" as varchar(65535))as remarks,
    t."lineageId",
    t."mofId"
  from
    sys_boot.jdbc_metadata.schemas_view_internal s
  inner join
    sys_fem.med."StoredTable" t
  on
    s."mofId" = t."namespace"
;

create or replace view dba_stored_tables_internal2 as
  select
    catalog_name,
    schema_name,
    table_name,
    creation_timestamp,
    last_modification_timestamp,
    last_analyze_row_count,
    last_analyze_timestamp,
    current_row_count,
    deleted_row_count,
    remarks,
    sti."lineageId",
    sti."mofId",
    g."Grantee"
  from
    dba_stored_tables_internal1 sti
  inner join
    sys_fem."Security"."Grant" g
  on
    sti."mofId" = g."Element"
  where
    g."action" = 'CREATION'
;

create or replace view dba_routines_internal1 as
  select
    cast(s.object_catalog as varchar(128)) as catalog_name,
    cast(s.object_schema as varchar(128)) as schema_name,
    cast(r."invocationName" as varchar(128)) as invocation_name,
    cast(r."name" as varchar(128)) as specific_name,
    cast(r."externalName" as varchar(65535)) as external_name,
    upper(r."type") as routine_type,
    cast(r."creationTimestamp" as timestamp) as creation_timestamp,
    cast(r."modificationTimestamp" as timestamp) as last_modified_timestamp,
    r."isUdx" as is_table_function,
    cast(r."parameterStyle" as varchar(128)) as parameter_style,
    r."deterministic" as is_deterministic,
    cast(r."dataAccess" as varchar(128)) as data_access,
    cast(r."description" as varchar(65535)) as remarks,
    r."mofId",
    r."lineageId"
  from
    sys_boot.jdbc_metadata.schemas_view_internal s
  inner join
    sys_fem."SQL2003"."Routine" r
  on
    s."mofId" = r."namespace"
;

create or replace view dba_routines_internal2 as
  select
    catalog_name,
    schema_name,
    invocation_name,
    specific_name,
    external_name,
    routine_type,
    creation_timestamp,
    last_modified_timestamp,
    is_table_function,
    parameter_style,
    is_deterministic,
    data_access,
    remarks,
    ri."mofId",
    ri."lineageId",
    g."Grantee"
  from
    dba_routines_internal1 ri
  inner join
    sys_fem."Security"."Grant" g
  on
    ri."mofId" = g."Element"
  where
    g."action" = 'CREATION'
;

create or replace view dba_routine_parameters_internal1 as
  select
    catalog_name,
    schema_name,
    specific_name as routine_specific_name,
    cast(rp."name" as varchar(128)) as parameter_name,
    rp."ordinal" as ordinal,
    coalesce(rp."length", rp."precision") as "PRECISION",
    rp."scale" as dec_digits,
    cast(rp."description" as varchar(65535)) as remarks,
    rp."mofId",
    rp."lineageId",
    rp."type",
    ri.is_table_function,
    ri.routine_type
  from
    dba_routines_internal1 ri
  inner join
    sys_fem."SQL2003"."RoutineParameter" rp
  on
    ri."mofId" = rp."behavioralFeature"
;

create or replace view dba_foreign_wrappers_internal as
  select
    cast(dw."name" as varchar(128)) as foreign_wrapper_name,
    cast(dw."libraryFile" as varchar(65535)) as library,
    cast(dw."language" as varchar(128)) as "LANGUAGE",
    cast(dw."creationTimestamp" as timestamp) as creation_timestamp,
    cast(dw."modificationTimestamp" as timestamp) last_modified_timestamp,
    cast(dw."description" as varchar(65535)) as remarks,
    g."Grantee",
    dw."mofId",
    dw."lineageId"
  from
    sys_fem.med."DataWrapper" dw
  inner join
    sys_fem."Security"."Grant" g
  on
    dw."mofId" = g."Element"
  where
    dw."foreign" = true and g."action" = 'CREATION'
;

create or replace view dba_foreign_servers_internal1 as
  select
    foreign_wrapper_name,
    cast(ds."name" as varchar(128)) as foreign_server_name,
    cast(ds."creationTimestamp" as timestamp) as creation_timestamp,
    cast(ds."modificationTimestamp" as timestamp) as last_modified_timestamp,
    cast(ds."description" as varchar(65535)) as remarks,
    ds."mofId",
    ds."lineageId"
  from
    dba_foreign_wrappers_internal fwi
  inner join
    sys_fem.med."DataServer" ds
  on
    fwi."mofId" = ds."Wrapper"
;

create or replace view dba_foreign_servers_internal2 as
  select
    foreign_wrapper_name,
    foreign_server_name,
    creation_timestamp,
    last_modified_timestamp,
    remarks,
    g."Grantee",
    fsi."mofId",
    fsi."lineageId"
  from
    dba_foreign_servers_internal1 fsi
  inner join
    sys_fem."Security"."Grant" g
  on
    fsi."mofId" = g."Element"
  where
    g."action" = 'CREATION'
;

create or replace view dba_foreign_tables_internal1 as
  select
    fs.foreign_wrapper_name,
    fs.foreign_server_name,
    cast(ft."name" as varchar(128))as foreign_table_name,
    cast(ft."creationTimestamp" as timestamp) as creation_timestamp,
    cast(ft."modificationTimestamp" as timestamp) as last_modified_timestamp,
    ft."lastAnalyzeRowCount" as last_analyze_row_count,
    cast(ft."analyzeTime" as timestamp) as last_analyze_timestamp,
    cast(ft."description" as varchar(65535)) as remarks,
    ft."mofId",
    ft."lineageId"
  from
    dba_foreign_servers_internal1 fs
  inner join
    sys_fem.med."ForeignTable" ft
  on
    fs."mofId" = ft."Server"
;

create or replace view dba_foreign_tables_internal2 as
  select
    fti.foreign_wrapper_name,
    fti.foreign_server_name,
    fti.foreign_table_name,
    fti.creation_timestamp,
    fti.last_modified_timestamp,
    fti.last_analyze_row_count,
    fti.last_analyze_timestamp,
    fti.remarks,
    g."Grantee",
    fti."mofId",
    fti."lineageId"
  from
    dba_foreign_tables_internal1 fti
  inner join
    sys_fem."Security"."Grant" g
  on
    fti."mofId" = g."Element"
  where
    g."action" = 'CREATION'
;

-- Returns the set of all foreign data wrappers which have been marked
-- as suitable for browse connect (mark is via the presence of the
-- BROWSE_CONNECT_DESCRIPTION option).
create or replace view browse_connect_foreign_wrappers as
  select
    dw."name" as foreign_wrapper_name,
    so."value" as browse_connect_description
  from
    sys_fem.med."DataWrapper" dw
  inner join
    sys_fem.med."StorageOption" so
  on
    dw."mofId" = so."StoredElement"
  where
    dw."foreign" = true
    and so."name" = 'BROWSE_CONNECT_DESCRIPTION'
;

create or replace view dba_labels_internal as
  select
    cast(lbl."name" as varchar(128)) as label_name,
    cast(pLbl."name" as varchar(128)) as parent_label_name,
    lbl."commitSequenceNumber" as csn,
    cast(lbl."creationTimestamp" as timestamp) as creation_timestamp,
    cast(lbl."modificationTimestamp" as timestamp) last_modified_timestamp,
    cast(lbl."description" as varchar(65535)) as remarks,
    lbl."mofId",
    lbl."lineageId",
    g."Grantee"
  from
    sys_fem.med."Label" lbl
  left outer join
    sys_fem.med."Label" pLbl
  on
    lbl."ParentLabel" = pLbl."mofId"
  inner join
    sys_fem."Security"."Grant" g
  on
    lbl."mofId" = g."Element"
  where
    g."action" = 'CREATION'
;


create or replace procedure create_directory(
  directory_path varchar(1024))
  language java
  parameter style java
  no sql
  external name
  'class net.sf.farrago.syslib.FarragoManagementUDR.createDirectory';

create or replace procedure delete_file_or_directory(
  file_path varchar(1024))
  language java
  parameter style java
  no sql
  external name
  'class net.sf.farrago.syslib.FarragoManagementUDR.deleteFileOrDirectory';

-- Tests that a connection can be established to a particular
-- SQL/MED local or foreign data server.
create or replace procedure test_data_server(
  server_name varchar(128))
  language java
  parameter style java
  no sql
  external name
  'class net.sf.farrago.syslib.FarragoMedUDR.testServer';

-- Tests that a connection can be established for all SQL/MED servers
-- instantiated from a particular data wrapper.
create or replace procedure test_all_servers_for_wrapper(
  wrapper_name varchar(128))
  language java
  parameter style java
  no sql
  external name
  'class net.sf.farrago.syslib.FarragoMedUDR.testAllServersForWrapper';

-- Returns the set of options relevant to a foreign server created
-- from a given wrapper.
--
-- A partial set of options may be passed in via the
-- proposed_server_options cursor parameter, which must have two
-- columns (OPTION_NAME and OPTION_VALUE, in that order).  This allows
-- for an incremental connection interaction, starting with specifying
-- no options, then some, then more, stopping once user and wrapper
-- are both satisfied.
--
-- The result set is not fully normalized, because some options
-- support a list of choices (e.g. for a dropdown selection UI
-- widget).  optional_choice_ordinal -1 represents the "current"
-- choice (either proposed by the user or chosen as default by the
-- wrapper); other choice ordinals starting from 0 represent possible
-- choices (if known).
create or replace function browse_connect_foreign_server(
  foreign_wrapper_name varchar(128),
  proposed_server_options cursor)
returns table(
  option_ordinal integer,
  option_name varchar(128),
  option_description varchar(4096),
  is_option_required boolean,
  option_choice_ordinal int,
  option_choice_value varchar(4096))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoMedUDR.browseConnectServer';

-- Returns the set of options relevant to a given wrapper.  The
-- wrapper must already have been registered in the catalog.
--
-- A partial set of options may be passed in via the
-- proposed_wrapper_options cursor parameter, which must have two
-- columns (OPTION_NAME and OPTION_VALUE, in that order).  This allows
-- for an incremental interaction, starting with specifying no
-- options, then some, then more, stopping once user and wrapper are
-- both satisfied. Whether or not any wrapper options are passed, the
-- wrapper's existing options stored in the catalog are ignored.
--
-- The locale_name parameter can either be the name of the locale for
-- which to return descriptions (e.g. 'en_US'), or NULL to use the
-- server's default locale.
--
-- The result set is not fully normalized, because some options
-- support a list of choices (e.g. for a dropdown selection UI
-- widget).  optional_choice_ordinal -1 represents the "current"
-- choice (either proposed by the user or chosen as default by the
-- wrapper); other choice ordinals starting from 0 represent possible
-- choices (if known).
create or replace function browse_wrapper(
  wrapper_name varchar(128),
  proposed_wrapper_options cursor,
  locale_name varchar(40))
returns table(
  option_ordinal integer,
  option_name varchar(128),
  option_description varchar(4096),
  is_option_required boolean,
  option_choice_ordinal int,
  option_choice_value varchar(4096))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoMedUDR.browseWrapper';

-- Returns the set of options relevant to a table created within a
-- given foreign data server.  The foreign data server must have
-- already been created.
--
-- A partial set of options may be passed in via the
-- proposed_table_options cursor parameter, which must have two
-- columns (OPTION_NAME and OPTION_VALUE, in that order).  This allows
-- for an incremental interaction, starting with specifying no
-- options, then some, then more, stopping once user and wrapper are
-- both satisfied.
--
-- The locale_name parameter can either be the name of the locale for
-- which to return descriptions (e.g. 'en_US'), or NULL to use the
-- server's default locale.
--
-- The result set is not fully normalized, because some options
-- support a list of choices (e.g. for a dropdown selection UI
-- widget).  optional_choice_ordinal -1 represents the "current"
-- choice (either proposed by the user or chosen as default by the
-- wrapper); other choice ordinals starting from 0 represent possible
-- choices (if known).
create or replace function browse_table(
  foreign_server_name varchar(128),
  proposed_table_options cursor,
  locale_name varchar(40))
returns table(
  option_ordinal integer,
  option_name varchar(128),
  option_description varchar(4096),
  is_option_required boolean,
  option_choice_ordinal int,
  option_choice_value varchar(4096))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoMedUDR.browseTable';

-- Returns the set of options relevant to a column created in a table
-- within a given foreign data server.  The foreign data server must
-- have already been created, but it is not necessary for the table or
-- column to exist.
--
-- A partial set of options may be passed in via the
-- proposed_column_options cursor parameter, which must have two
-- columns (OPTION_NAME and OPTION_VALUE, in that order).  This allows
-- for an incremental interaction, starting with specifying no
-- options, then some, then more, stopping once user and wrapper are
-- both satisfied.
--
-- Likewise, the table_options cursor parameter contains the options
-- of the table.
--
-- The locale_name parameter can either be the name of the locale for
-- which to return descriptions (e.g. 'en_US'), or NULL to use the
-- server's default locale.
--
-- The result set is not fully normalized, because some options
-- support a list of choices (e.g. for a dropdown selection UI
-- widget).  optional_choice_ordinal -1 represents the "current"
-- choice (either proposed by the user or chosen as default by the
-- wrapper); other choice ordinals starting from 0 represent possible
-- choices (if known).
create or replace function browse_column(
  foreign_server_name varchar(128),
  proposed_table_options cursor,
  proposed_column_options cursor,
  locale_name varchar(40))
returns table(
  option_ordinal integer,
  option_name varchar(128),
  option_description varchar(4096),
  is_option_required boolean,
  option_choice_ordinal int,
  option_choice_value varchar(4096))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoMedUDR.browseColumn';

-- A view which can be used as the input cursor for proposed_server_options
-- when no options are set (initial browse).
create or replace view browse_connect_empty_options as
select '' as option_name, '' as option_value
from sys_boot.jdbc_metadata.empty_view;

-- A table function that returns same as the browse_connect_empty_options
-- view. Workaround for dtbug 2387, "UDX with cursor inputs hangs". Remove when
-- that bug is fixed.
create or replace function browse_connect_empty_options_udx()
returns table(
  option_name varchar(128),
  option_value varchar(128))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoMedUDR.browseEmptyOptions';

-- Returns foreign schemas accessible via a given foreign server.
create or replace function browse_foreign_schemas(
  foreign_server_name varchar(128))
returns table(
  schema_name varchar(128),
  description varchar(4096))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoMedUDR.browseForeignSchemas';

--
-- Datetime conversion functions
--

-- converts a string to a date, according to the specified format string
create or replace function char_to_date(format varchar(50), dateString varchar(128))
returns date
language java
specific std_char_to_date
no sql
external name 'class net.sf.farrago.syslib.FarragoConvertDatetimeUDR.char_to_date';

create or replace function char_to_time(format varchar(50), timeString varchar(128))
returns time
language java
specific std_char_to_time
no sql
external name 'class net.sf.farrago.syslib.FarragoConvertDatetimeUDR.char_to_time';

create or replace function char_to_timestamp(
     format varchar(50), timestampString varchar(128))
returns timestamp
language java
specific std_char_to_timestamp
no sql
external name 'class net.sf.farrago.syslib.FarragoConvertDatetimeUDR.char_to_timestamp';

-- formats a string as a date, according to the specified format string
create or replace function date_to_char(format varchar(50), d date)
returns varchar(128)
language java
specific std_date_to_char
no sql
external name 'class net.sf.farrago.syslib.FarragoConvertDatetimeUDR.date_to_char';

create or replace function time_to_char(format varchar(50), t time)
returns varchar(128)
language java
specific std_time_to_char
no sql
external name 'class net.sf.farrago.syslib.FarragoConvertDatetimeUDR.time_to_char';

create or replace function timestamp_to_char(format varchar(50), ts timestamp)
returns varchar(128)
language java
specific std_timestamp_to_char
no sql
external name 'class net.sf.farrago.syslib.FarragoConvertDatetimeUDR.timestamp_to_char';

create or replace function perforce_changelists(
  file_pattern varchar(2048),
  maxChanges integer)
returns table(
  changelist_number varchar(128),
  changelist_date varchar(128),
  submitter varchar(128),
  perforce_client varchar(128),
  change_description varchar(2048))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoPerforceUDR.getChangelists';

create or replace view perforce_submitters as
select distinct submitter
from table(perforce_changelists('.../dev/f...', -1));

-- retrieves a long catalog string attribute in chunks
create or replace function repository_lob_text(
  mof_id varchar(128),
  attribute_name varchar(128))
returns table(
  chunk_offset integer,
  chunk_text varchar(1024))
language java
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoManagementUDR.lobText';

-- returns a create statement for every item in a given schema
create or replace function generate_ddl_for_schema(
  schema_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_schema
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForSchema';

-- use other catalogs
create or replace function generate_ddl_for_schema(
  catalog_name varchar(128),
  schema_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_schema2
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForSchema';

-- ddl dumps everything in current catalog
create or replace function generate_ddl_for_catalog()
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_catalog
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForCatalog';

-- use other catalogs
create or replace function generate_ddl_for_catalog(catalog_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_catalog2
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForCatalog';

-- ddl for tables and views
create or replace function generate_ddl_for_table(
  schema_name varchar(128),
  table_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_table
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForTable';

-- use other catalogs
create or replace function generate_ddl_for_table(
  catalog_name varchar(128),
  schema_name varchar(128),
  table_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_table2
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForTable';

-- ddl for generic procedures and functions (returns all matches)
create or replace function generate_ddl_for_routine(
  schema_name varchar(128),
  routine_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_routine
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForRoutine';

-- use other catalogs
create or replace function generate_ddl_for_routine(
  catalog_name varchar(128),
  schema_name varchar(128),
  routine_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_routine2
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForRoutine';

-- used when specific name is desired
create or replace function generate_ddl_for_specific_routine(
  schema_name varchar(128),
  routine_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_specific_routine
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForSpecificRoutine';

-- use other catalogs
create or replace function generate_ddl_for_specific_routine(
  catalog_name varchar(128),
  schema_name varchar(128),
  routine_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_specific_routine2
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForSpecificRoutine';

-- ddl for jars
create or replace function generate_ddl_for_jar(
  schema_name varchar(128),
  jar_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_jar
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForJar';

-- use other catalogs
create or replace function generate_ddl_for_jar(
  catalog_name varchar(128),
  schema_name varchar(128),
  jar_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_jar2
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForJar';

-- ddl for servers
create or replace function generate_ddl_for_server(server_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_server
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForServer';

-- ddl for wrappers
create or replace function generate_ddl_for_wrapper(wrapper_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_wrapper
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForWrapper';

-- ddl for indexes
create or replace function generate_ddl_for_index(
  schema_name varchar(128),
  index_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_index
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForIndex';

-- use other catalogs
create or replace function generate_ddl_for_index(
  catalog_name varchar(128),
  schema_name varchar(128),
  index_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_index2
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForIndex';

-- ddl for users
create or replace function generate_ddl_for_user(user_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_user
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForUser';

-- ddl for roles
create or replace function generate_ddl_for_role(role_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_role
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForRole';

-- ddl for labels
create or replace function generate_ddl_for_label(label_name varchar(128))
returns table(
  statement varchar(65535))
language java
specific generate_ddl_for_label
parameter style system defined java
no sql
external name
'class net.sf.farrago.syslib.FarragoDdlViewUDR.generateForLabel';

-- variation of LucidDB session personality but with index only scans enabled
create or replace jar sys_boot.sys_boot.luciddb_index_only_plugin
library 'class org.luciddb.session.LucidDbIndexOnlySessionFactory'
options(0);

CREATE or replace FUNCTION get_plugin_property_info(
  mofId varchar(65535),
  libraryName varchar(65535),
  wrapper_options cursor,
  locale varchar(65535))
returns table(
  option_ordinal integer,
  option_name varchar(128),
  option_description varchar(4096),
  is_option_required boolean,
  option_choice_ordinal int,
  option_choice_value varchar(4096))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoMedUDR.getPluginPropertyInfo';

CREATE or replace FUNCTION get_server_property_info(
  mofId varchar(65535),
  libraryName varchar(65535),
  wrapper_options cursor,
  proposed_server_options cursor,
  locale varchar(65535))
returns table(
  option_ordinal integer,
  option_name varchar(128),
  option_description varchar(4096),
  is_option_required boolean,
  option_choice_ordinal int,
  option_choice_value varchar(4096))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoMedUDR.getServerPropertyInfo';

CREATE or replace FUNCTION get_keywords()
returns table(
  keyword varchar(128))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoSqlAdvisorUDR.getReservedAndKeyWords';

CREATE or replace FUNCTION validate_sql(
  sql_statement varchar(65535))
returns table(
  start_line_num integer,
  start_column_num integer,
  end_line_num integer,
  end_column_num integer,
  message varchar(4096))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoSqlAdvisorUDR.validate';

CREATE or replace FUNCTION is_valid_sql(
  sql_statement varchar(65535))
returns boolean
language java parameter style java no sql
external name 'class net.sf.farrago.syslib.FarragoSqlAdvisorUDR.isValid';

CREATE or replace FUNCTION complete_sql(
  sql_statement varchar(65535),
  offset integer)
returns table(
  item_type varchar(16),
  item_name varchar(128))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoSqlAdvisorUDR.complete';

CREATE or replace FUNCTION format_sql(
  sql_chunks cursor,
  always_use_parentheses boolean,
  case_clauses_on_new_lines boolean,
  clause_starts_line boolean,
  keywords_lowercase boolean,
  quote_all_identifiers boolean,
  select_list_items_on_separate_lines boolean,
  where_list_items_on_separate_lines boolean,
  window_declaration_starts_line boolean,
  window_list_items_on_separate_lines boolean,
  indentation integer,
  line_length integer)
returns table(
  chunk_idx integer,
  formatted_sql varchar(65535))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoSqlAdvisorUDR.format';

CREATE or replace FUNCTION get_lurql_xmi(
  lurql_query varchar(65535))
returns table(
  chunk_idx integer,
  xmi_result varchar(32767))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoLurqlUDR.getXMI';

CREATE or replace FUNCTION get_metamodel_xmi(
  extent_name varchar(65535))
returns table(
  chunk_idx integer,
  xmi_result varchar(32767))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoLurqlUDR.getMetamodelXMI';

CREATE or replace FUNCTION get_lurql_json(
  lurql_query varchar(65535))
returns table(
  chunk_idx integer,
  json_result varchar(32767))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoLurqlUDR.getJSON';

CREATE or replace FUNCTION get_lurql_names(
  lurql_query varchar(65535))
returns table(object_name varchar(32767))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoLurqlUDR.getObjectNames';

CREATE or replace FUNCTION get_object_ddl(
  catalog_name varchar(1024),
  schema_name varchar(1024),
  object_name varchar(1024))
returns table(
  chunk_index integer,
  ddl varchar(32767))
language java parameter style system defined java no sql
external name 'class net.sf.farrago.syslib.FarragoLurqlUDR.getObjectDdl';

-- End createMgmtViews.sql
