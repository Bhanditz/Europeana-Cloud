USE %s;
CREATE TABLE IF NOT EXISTS data_providers (provider_id varchar PRIMARY KEY,properties map<varchar, text>, data_sets map<varchar, varchar>, creation_date timestamp, partition_key int);
CREATE TABLE IF NOT EXISTS Cloud_Id(cloud_id varchar, provider_id varchar, record_id varchar, PRIMARY KEY (cloud_id, provider_id,record_id));
CREATE TABLE IF NOT EXISTS Provider_Record_Id(provider_id varchar, record_id varchar, cloud_id varchar, PRIMARY KEY (provider_id,record_id));
