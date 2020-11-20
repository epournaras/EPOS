DROP TABLE IF EXISTS UnfairnessLogger CASCADE;

CREATE TABLE UnfairnessLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,run INTEGER NOT NULL
	,peer INTEGER NOT NULL
	,iteration INTEGER NOT NULL
	,unfairness INTEGER NOT NULL
);

