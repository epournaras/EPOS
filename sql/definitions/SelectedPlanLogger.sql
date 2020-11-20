DROP TABLE IF EXISTS SelectedPlanLogger CASCADE;

CREATE TABLE SelectedPlanLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,run INTEGER NOT NULL
	,peer INTEGER NOT NULL
	,iteration INTEGER NOT NULL
	,planID INTEGER NOT NULL
);

