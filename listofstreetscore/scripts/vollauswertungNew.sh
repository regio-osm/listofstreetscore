#/usr/bin/bash
cd /home/osm/apps/listofstreetscore/listofstreetscore/src
rm ../prod_update_streetlist_complete.log-old
mv ../prod_update_streetlist_complete.log ../prod_update_streetlist_complete.log-old
rm ../log_message_Streetlist_from_streetlistwiki.txt
mv ../log_message_Streetlist_from_streetlistwiki.txt-old ../log_message_Streetlist_from_streetlistwiki.txt
rm ../log_message_get_streetlist_from_streetlistwiki.txt-old
mv ../log_message_get_streetlist_from_streetlistwiki.txt ../log_message_get_streetlist_from_streetlistwiki.txt-old
echo "read wiki in complete mode ..."
/usr/lib/jvm/java-7-openjdk-amd64/bin/java -cp .:/home/osm/software/postgresql.jar StreetlistWikiReader -mode complete >"../prod_update_streetlist_complete.log" 2>&1
echo "completed read wiki in complete mode"
rm ../prod_update_streetlist_recentchanges.log-old
mv ../prod_update_streetlist_recentchanges.log ../prod_update_streetlist_recentchanges.log-old
echo "read wiki in recentchange mode ..."
/usr/lib/jvm/java-7-openjdk-amd64/bin/java -cp .:/home/osm/software/postgresql.jar StreetlistWikiReader -mode recentchanges >"../prod_update_streetlist_recentchanges.log" 2>&1
echo "complete read wiki in recentchange mode"
rm ../prod_create_muni.log-old
mv ../prod_create_muni.log ../prod_create_muni.log-old
rm ../log_message_create_municipality_polygons.txt-old
mv ../log_message_create_municipality_polygons.txt ../log_message_create_municipality_polygons.txt-old
echo "create municipality polygons ..."
/usr/lib/jvm/java-7-openjdk-amd64/bin/java -cp .:/home/osm/software/postgresql.jar MunicipalityPolygons >"../prod_create_muni.log" 2>&1
echo "complete create municipality polygons"
rm ../prod_evaluation.log-old
mv ../prod_evaluation.log ../prod_evaluation.log-old
cd /home/osm/apps/listofstreetsclient/listofstreetsclient/src
/usr/lib/jvm/java-7-openjdk-amd64/bin/java -cp .:/home/osm/software/postgresql.jar  EvaluationNew -evaluationtype full >"/home/osm/apps/listofstreetscore/listofstreetscore/prod_evaluation.log" 2>&1
#/usr/lib/jvm/java-7-openjdk-amd64/bin/java -cp .:/home/osm/software/postgresql.jar  EvaluationNew -name Augsburg >"/home/osm/apps/listofstreetscore/listofstreetscore/prod_TESTevaluation.log" 2>&1
cd /home/osm/apps/listofstreetscore/listofstreetscore/src
#update table for map
mv ../prod_update_table_muniview.log ../prod_update_table_muniview.log-old
psql -U okilimu -d u_okilimu -f update_table_muniview.sql >"../prod_update_table_muniview.log" 2>&1
#rm prod_add_street_points.log-old
#mv prod_add_street_points.log prod_add_street_points.log-old
#/usr/lib/jvm/java-7-openjdk-amd64/bin/java -cp .:/home/osm/software/postgresql.jar add_street_points > prod_add_street_points.log
