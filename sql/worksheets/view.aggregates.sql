-- number of rows in the table
SELECT COUNT(*) FROM aggregation;


-- get the last N rows in the table
--EXPLAIN
SELECT * FROM aggregation WHERE seq_id >= (SELECT MAX(seq_id) FROM aggregation) - 20;


-- show the last aggregation state per peer
--EXPLAIN
WITH with_last_peer_seq_id AS
(
    SELECT
        MAX(seq_id) AS last_peer_seq_id
        ,peer
    FROM
        aggregation
    GROUP BY    
        peer
)
/* SELECT * FROM with_last_peer_seq_id ORDER BY peer ASC; */
SELECT
    agg.*
FROM
    aggregation agg
INNER JOIN 
    with_last_peer_seq_id last_records
    ON
    last_records.last_peer_seq_id = agg.seq_id
ORDER BY
    peer ASC
;


        

