registerHandler("GET", "/lastExecution", function(out){
    
    try {
        persistenceEngine.initializeClass("Alert");
        var result = persistenceEngine.query("select max(timestamp) as lastExecution from Alert",null);
        out.write(new java.lang.String(result.get("object")).getBytes("utf-8"));
    } catch (e) {
        print(e);
    }
});

registerHandler("GET", "/getAlertsByGroup", function(out,pathParams,requestParams){
    
    try {
        persistenceEngine.initializeClass("Alert");
        var params = new Packages.java.util.HashMap();
        params.put("group", requestParams.getStringParameter("group",""));
        var results = persistenceEngine.query("select * from Alert where group = :group",params);
        out.write(new java.lang.String(results).getBytes("utf-8"));
    } catch (e) {
        print(e);
    }
});


registerHandler("GET", "/getCdaErrors", function(out){
    
    try {
        
        persistenceEngine.initializeClass("cdaEvent");
        var results = persistenceEngine.query("select timestamp, "+
            "queryInfo.cdaSettingsId.append(\"[\").append(queryInfo.dataAccessId).append(\"]\") as file, queryInfo.parameters as parameters , "+
            "message, @rid from cdaEvent where eventType = 'QueryError'  order by timestamp desc limit 100",null);
        
        //console.log("Results: " + results.toJSON());
        
        var object = JSON.parse(results.getJSONArray("object").toString());
        _.map(object,function(l){
            
            // Add timestamp group
            l.timestampGroup = wd.cdv.utils.groupTimestamp(l.timestamp,2);
            
            
            // Put the files as we need them

            var params = _.flatten(l.parameters);
            var paramsArray = [];
            for(i = 0; i< params.length/2; i++){
                paramsArray.push([params[i*2],params[i*2+1]]); 
            }
    
            if(paramsArray.length > 0){
                var paramStr = _.map(paramsArray,function(m){
                    return m.join(": ")
                }).join(", ");
                l.file = l.file + " (" + paramStr + ")";
            }
            delete l.parameters;
            return l;
  
        });

        out.write(new java.lang.String(JSON.stringify(object,null,2)).getBytes("utf-8"));
        
        
    } catch (e) {
        print(e);
    }
});



registerHandler("GET", "/getCdaSlowQueries", function(out){
    
    try {
        
        persistenceEngine.initializeClass("cdaEvent");
        var results = persistenceEngine.query("select timestamp, "+
            "queryInfo.cdaSettingsId.append(\"[\").append(queryInfo.dataAccessId).append(\"]\") as file, queryInfo.parameters as parameters , "+
            "duration, @rid from cdaEvent where eventType = 'QueryTooLong'  order by timestamp desc limit 100",null);
        
        //console.log("Results: " + results.toJSON());
        
        var object = JSON.parse(results.getJSONArray("object").toString());
        _.map(object,function(l){
            
            // Add timestamp group
            l.timestampGroup = wd.cdv.utils.groupTimestamp(l.timestamp,2);
            
            
            // Put the files as we need them

            var params = _.flatten(l.parameters);
            var paramsArray = [];
            for(i = 0; i< params.length/2; i++){
                paramsArray.push([params[i*2],params[i*2+1]]); 
            }
    
            if(paramsArray.length > 0){
                var paramStr = _.map(paramsArray,function(m){
                    return m.join(": ")
                }).join(", ");
                l.file = l.file + " (" + paramStr + ")";
            }
            delete l.parameters;
            return l;
  
        });

        out.write(new java.lang.String(JSON.stringify(object,null,2)).getBytes("utf-8"));
        
        
    } catch (e) {
        print(e);
    }
});


registerHandler("GET", "/deleteCdaEntry", function(out,pathParams,requestParams){
    

    try {
        
        console.log("deleteCdaEntry method ");
        persistenceEngine.initializeClass("cdaEvent");
        var params = new Packages.java.util.HashMap();
        var cdaEntryId = requestParams.getStringParameter("cdaEntryId","")
        params.put("cdaEntryId", cdaEntryId);
        
        // 1. Find queryInfo.@rid
        var queryInfoRid = JSON.parse(persistenceEngine.query("select queryInfo from cdaEvent where  @rid = :cdaEntryId ", params).getJSONArray("object").toString())[0].queryInfo;
        console.log("Deleting queryInfoRid: " + queryInfoRid);
        persistenceEngine.deleteRecord(queryInfoRid);
        persistenceEngine.deleteRecord(cdaEntryId);
        
        out.write(new java.lang.String("{result: true}").getBytes("utf-8"));

        /*
        var results = persistenceEngine.query("delete from ( traverse * from (select from cdaEvent where  @rid = \"" + cdaEntryId + "\" ) where  $depth <= 1 )", params);
        out.write(new java.lang.String(results).getBytes("utf-8"));
        */
       
    } catch (e) {
        print(e);
    }
});


registerHandler("GET", "/deleteCdaEntriesOfEventType", function(out,pathParams,requestParams){
    

    try {
        
        persistenceEngine.initializeClass("cdaEvent");
        var params = new Packages.java.util.HashMap();
        var eventType = requestParams.getStringParameter("eventType","")
        params.put("eventType", eventType);
        
        console.log("Deleting eventTypes: " + eventType);

        // 1. Find queryInfo.@rid
        var result = persistenceEngine.command("delete from cdaEvent where eventType = :eventType ", params)
        out.write(new java.lang.String(result).getBytes("utf-8"));
        
    } catch (e) {
        print(e);
    }
});