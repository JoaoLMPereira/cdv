/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. 
 *  
 */

var wd = wd || {};
wd.cdvUI = wd.cdvUI ||{
    

    siteMap: [
    {
        name: "Home",
        link: "home",
        sublinks: []
    },
    {
        name: "Validations",
        link: "validations",
        sublinks: []
    },
    {
        name: "CDA Errors",
        link: "cdaErrors",
        sublinks: []
    },
    {
        name: "Slow Queries",
        link: "slowQueries",
        sublinks: []
    },
    {
        name: "About",
        link: "about",
        sublinks: []
    },

    ]
    
    
    
};


wd.cdvUI.validationTablePreExec = function(){
    
    
    // Configure addIns
    
    
    }


wd.cdvUI.validationFileAddin = {
    name: "validationFile",
    label: "ValidationFile",
    defaults: {
        
    },
    init: function(){
        
        // Register this for datatables sort
        var myself = this;
        $.fn.dataTableExt.oSort[this.name+'-asc'] = function(a,b){
            return myself.sort(a,b)
        };
        $.fn.dataTableExt.oSort[this.name+'-desc'] = function(a,b){
            return myself.sort(b,a)
        };     
    }, 
    sort: function(a,b){
        return this.sumStrArray(a) - this.sumStrArray(b);
    }, 
    sumStrArray: function(arr){
        return arr.split(',').reduce(function(prev, curr, index, array){  
            console.log("Current " + curr +"; prev " +  prev); 
            return parseFloat(curr) + (typeof(prev)==='number'?prev:parseFloat(prev));
        });
    },

    implementation: function (tgt, st, opt) {
        
        // encapsulate this
        
        var $t = $(tgt);
        var text = st.value;
        
        if($t.find("div.validationFileWrapper").length>0){
            return; // Done already
        }
        
        var val = _.map(text.split(" ;"),function(e){
    
            var obj = {};
    
            var arr = e.match(/(.*)\[(.*)] (.*)/);
            obj.cda = arr[1];
            obj.dataAccessId = arr[2];
    
            var pathArray = obj.cda.split("/")
            var a = pathArray.slice(0,pathArray.length-1);
            obj.file = pathArray.slice(pathArray.length - 1)[0];
            obj.path = pathArray.slice(0,pathArray.length - 1).join("/")+"/";
            
            
            obj.pathDesc = (a.length>3?a.slice(0,2).concat([".."]).concat(a.slice(a.length-1,a.length)):a).join("/")+"/"
    
            var params = arr[3];
            if(params){
                obj.params = _.map(params.substr(2,params.length-3).split(", "), function(param){
                    var a = param.split(": ");
                    return {
                        paramName: a[0], 
                        paramValue: a[1]
                    } ;
                });
        
            }
            
            obj.paramLink = function(){
                if(!obj.params){
                    return "";
                }
                
                var n = obj.params.length;
                var template = " <a title='" + _.map(obj.params,function(p){
                    return p.paramName+": "+p.paramValue;
                    }).join("<br />") + "' class='params'>(" + n + " param" + (n>1?"s":"") +")</a>";
                
                return template;
            };
            
            return obj;
    
        });

        var template = "<div class='validationFileWrapper'>{{#val}}<div class='cda'><div class='cdaFile'>" +
        "<span class='cdaPath' title='{{cda}}'>{{pathDesc}}</span><span>{{file}}</span><span class='dataAccessId'>:{{dataAccessId}}</span>"+
        "<span class='paramLink'>{{{paramLink}}}</span></div>" +
        // "</div>{{#params}}<div class='params'><div class='paramKey'>{{paramName}}" +
        // "</div><div class='paramValue'>{{paramValue}}</div></div>{{/params}}" +
        "</div>{{/val}}</div>";

        $t.html(Mustache.render(template, {
            val: val
            
        }));
        
        $t.find("a.params").tipsy({gravity: 's', html:true});
        $t.find("span.cdaPath").tipsy({gravity: 's', html:true});
    // 
    }
};

Dashboards.registerAddIn("Table", "colType", new AddIn(wd.cdvUI.validationFileAddin));