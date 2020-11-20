DROP TABLE IF EXISTS TerminationLogger CASCADE;

CREATE TABLE TerminationLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,run INTEGER NOT NULL
	,peer INTEGER NOT NULL
	,termination INTEGER NOT NULL
);

