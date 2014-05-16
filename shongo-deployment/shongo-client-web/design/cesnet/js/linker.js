/*! Snap.js v2.0.0-rc1 */
(function(a,b){"use strict";var c="Snap",d={extend:function(a,b){var c;for(c in b)b[c]&&b[c].constructor&&b[c].constructor===Object?(a[c]=a[c]||{},d.extend(a[c],b[c])):a[c]=b[c];return a}},e=function(c){var e=this,f=e.settings={element:null,dragger:null,disable:"none",addBodyClasses:!0,hyperextensible:!0,resistance:.5,flickThreshold:50,transitionSpeed:.3,easing:"ease",maxPosition:266,minPosition:-266,tapToClose:!0,touchToDrag:!0,clickToDrag:!0,slideIntent:40,minDragDistance:5},g=e.cache={isDragging:!1,simpleStates:{opening:null,towards:null,hyperExtending:null,halfway:null,flick:null,translation:{absolute:0,relative:0,sinceDirectionChange:0,percentage:0}}},h=e.eventList={};d.extend(d,{hasTouch:"ontouchstart"in b.documentElement||a.navigator.msPointerEnabled,eventType:function(a){var b={down:d.hasTouch?"touchstart":f.clickToDrag?"mousedown":"",move:d.hasTouch?"touchmove":f.clickToDrag?"mousemove":"",up:d.hasTouch?"touchend":f.clickToDrag?"mouseup":"",out:d.hasTouch?"touchcancel":f.clickToDrag?"mouseout":""};return b[a]},page:function(a,b){return d.hasTouch&&b.touches.length&&b.touches[0]?b.touches[0]["page"+a]:b["page"+a]},klass:{has:function(a,b){return-1!==a.className.indexOf(b)},add:function(a,b){!d.klass.has(a,b)&&f.addBodyClasses&&(a.className+=" "+b)},remove:function(a,b){d.klass.has(a,b)&&f.addBodyClasses&&(a.className=a.className.replace(b,"").replace(/^\s+|\s+$/g,""))}},dispatchEvent:function(a){return"function"==typeof h[a]?h[a].apply():void 0},vendor:function(){var a,c=b.createElement("div"),d="webkit Moz O ms".split(" ");for(a in d)if("undefined"!=typeof c.style[d[a]+"Transition"])return d[a]},transitionCallback:function(){return"Moz"===g.vendor||"ms"===g.vendor?"transitionend":g.vendor+"TransitionEnd"},canTransform:function(){return"undefined"!=typeof f.element.style[g.vendor+"Transform"]},angleOfDrag:function(a,b){var c,d;return d=Math.atan2(-(g.startDragY-b),g.startDragX-a),0>d&&(d+=2*Math.PI),c=Math.floor(d*(180/Math.PI)-180),0>c&&c>-180&&(c=360-Math.abs(c)),Math.abs(c)},events:{addEvent:function(a,b,c){return a.addEventListener?a.addEventListener(b,c,!1):a.attachEvent?a.attachEvent("on"+b,c):void 0},removeEvent:function(a,b,c){return a.addEventListener?a.removeEventListener(b,c,!1):a.attachEvent?a.detachEvent("on"+b,c):void 0},prevent:function(a){a.preventDefault?a.preventDefault():a.returnValue=!1}},parentUntil:function(a,b){for(var c="string"==typeof b;a.parentNode;){if(c&&a.getAttribute&&a.getAttribute(b))return a;if(!c&&a===b)return a;a=a.parentNode}return null}});var i=e.action={translate:{get:{matrix:function(b){if(g.canTransform){var c=a.getComputedStyle(f.element)[g.vendor+"Transform"].match(/\((.*)\)/),d=8;return c?(c=c[1].split(","),16===c.length&&(b+=d),parseInt(c[b],10)):0}return parseInt(f.element.style.left,10)}},easeCallback:function(){f.element.style[g.vendor+"Transition"]="",g.translation=i.translate.get.matrix(4),g.easing=!1,0===g.easingTo&&(d.klass.remove(b.body,"snapjs-right"),d.klass.remove(b.body,"snapjs-left")),g.once&&(g.once.call(e,e.state()),delete g.once),d.dispatchEvent("animated"),d.events.removeEvent(f.element,d.transitionCallback(),i.translate.easeCallback)},easeTo:function(a,b){g.canTransform?(g.easing=!0,g.easingTo=a,f.element.style[g.vendor+"Transition"]="all "+f.transitionSpeed+"s "+f.easing,g.once=b,d.events.addEvent(f.element,d.transitionCallback(),i.translate.easeCallback),i.translate.x(a)):(g.translation=a,i.translate.x(a)),0===a&&(f.element.style[g.vendor+"Transform"]="")},x:function(c){if(!("left"===f.disable&&c>0||"right"===f.disable&&0>c))if(f.hyperextensible||(c===f.maxPosition||c>f.maxPosition?c=f.maxPosition:(c===f.minPosition||c<f.minPosition)&&(c=f.minPosition)),c=parseInt(c,10),isNaN(c)&&(c=0),g.canTransform){var d="translate3d("+c+"px, 0,0)";f.element.style[g.vendor+"Transform"]=d}else f.element.style.width=(a.innerWidth||b.documentElement.clientWidth)+"px",f.element.style.left=c+"px",f.element.style.right=""}},drag:{listen:function(){g.translation=0,g.easing=!1,d.events.addEvent(e.settings.element,d.eventType("down"),i.drag.startDrag),d.events.addEvent(e.settings.element,d.eventType("move"),i.drag.dragging),d.events.addEvent(e.settings.element,d.eventType("up"),i.drag.endDrag)},stopListening:function(){d.events.removeEvent(f.element,d.eventType("down"),i.drag.startDrag),d.events.removeEvent(f.element,d.eventType("move"),i.drag.dragging),d.events.removeEvent(f.element,d.eventType("up"),i.drag.endDrag)},startDrag:function(a){var b=a.target?a.target:a.srcElement,c=d.parentUntil(b,"data-snap-ignore");if(c)return void d.dispatchEvent("ignore");if(f.dragger){var e=d.parentUntil(b,f.dragger);if(!e&&g.translation!==f.minPosition&&g.translation!==f.maxPosition)return}d.dispatchEvent("start"),f.element.style[g.vendor+"Transition"]="",g.isDragging=!0,g.intentChecked=!1,g.startDragX=d.page("X",a),g.startDragY=d.page("Y",a),g.dragWatchers={current:0,last:0,hold:0,state:""},g.simpleStates={opening:null,towards:null,hyperExtending:null,halfway:null,flick:null,translation:{absolute:0,relative:0,sinceDirectionChange:0,percentage:0}}},dragging:function(a){if(g.isDragging&&f.touchToDrag){var c,e=d.page("X",a),h=d.page("Y",a),j=g.translation,k=i.translate.get.matrix(4),l=e-g.startDragX,m=k>0,n=l;if(g.intentChecked&&!g.hasIntent)return;if(f.addBodyClasses&&(k>0?(d.klass.add(b.body,"snapjs-left"),d.klass.remove(b.body,"snapjs-right")):0>k&&(d.klass.add(b.body,"snapjs-right"),d.klass.remove(b.body,"snapjs-left"))),g.hasIntent===!1||null===g.hasIntent){var o=d.angleOfDrag(e,h),p=o>=0&&o<=f.slideIntent||360>=o&&o>360-f.slideIntent,q=o>=180&&o<=180+f.slideIntent||180>=o&&o>=180-f.slideIntent;g.hasIntent=q||p?!0:!1,g.intentChecked=!0}if(f.minDragDistance>=Math.abs(e-g.startDragX)||g.hasIntent===!1)return;d.events.prevent(a),d.dispatchEvent("drag"),g.dragWatchers.current=e,g.dragWatchers.last>e?("left"!==g.dragWatchers.state&&(g.dragWatchers.state="left",g.dragWatchers.hold=e),g.dragWatchers.last=e):g.dragWatchers.last<e&&("right"!==g.dragWatchers.state&&(g.dragWatchers.state="right",g.dragWatchers.hold=e),g.dragWatchers.last=e),m?(f.maxPosition<k&&(c=(k-f.maxPosition)*f.resistance,n=l-c),g.simpleStates={opening:"left",towards:g.dragWatchers.state,hyperExtending:f.maxPosition<k,halfway:k>f.maxPosition/2,flick:Math.abs(g.dragWatchers.current-g.dragWatchers.hold)>f.flickThreshold,translation:{absolute:k,relative:l,sinceDirectionChange:g.dragWatchers.current-g.dragWatchers.hold,percentage:k/f.maxPosition*100}}):(f.minPosition>k&&(c=(k-f.minPosition)*f.resistance,n=l-c),g.simpleStates={opening:"right",towards:g.dragWatchers.state,hyperExtending:f.minPosition>k,halfway:k<f.minPosition/2,flick:Math.abs(g.dragWatchers.current-g.dragWatchers.hold)>f.flickThreshold,translation:{absolute:k,relative:l,sinceDirectionChange:g.dragWatchers.current-g.dragWatchers.hold,percentage:k/f.minPosition*100}}),i.translate.x(n+j)}},endDrag:function(a){if(g.isDragging){d.dispatchEvent("end");var b=i.translate.get.matrix(4);if(0===g.dragWatchers.current&&0!==b&&f.tapToClose)return d.dispatchEvent("close"),d.events.prevent(a),i.translate.easeTo(0),g.isDragging=!1,void(g.startDragX=0);"left"===g.simpleStates.opening?g.simpleStates.halfway||g.simpleStates.hyperExtending||g.simpleStates.flick?g.simpleStates.flick&&"left"===g.simpleStates.towards?i.translate.easeTo(0):(g.simpleStates.flick&&"right"===g.simpleStates.towards||g.simpleStates.halfway||g.simpleStates.hyperExtending)&&i.translate.easeTo(f.maxPosition):i.translate.easeTo(0):"right"===g.simpleStates.opening&&(g.simpleStates.halfway||g.simpleStates.hyperExtending||g.simpleStates.flick?g.simpleStates.flick&&"right"===g.simpleStates.towards?i.translate.easeTo(0):(g.simpleStates.flick&&"left"===g.simpleStates.towards||g.simpleStates.halfway||g.simpleStates.hyperExtending)&&i.translate.easeTo(f.minPosition):i.translate.easeTo(0)),g.isDragging=!1,g.startDragX=d.page("X",a)}}}};c.element&&(d.extend(f,c),g.vendor=d.vendor(),g.canTransform=d.canTransform(),i.drag.listen())};d.extend(e.prototype,{open:function(a,c){d.dispatchEvent("open"),d.klass.remove(b.body,"snapjs-expand-left"),d.klass.remove(b.body,"snapjs-expand-right"),"left"===a?(this.cache.simpleStates.opening="left",this.cache.simpleStates.towards="right",d.klass.add(b.body,"snapjs-left"),d.klass.remove(b.body,"snapjs-right"),this.action.translate.easeTo(this.settings.maxPosition,c)):"right"===a&&(this.cache.simpleStates.opening="right",this.cache.simpleStates.towards="left",d.klass.remove(b.body,"snapjs-left"),d.klass.add(b.body,"snapjs-right"),this.action.translate.easeTo(this.settings.minPosition,c))},close:function(a){d.dispatchEvent("close"),this.action.translate.easeTo(0,a)},expand:function(c){var e=a.innerWidth||b.documentElement.clientWidth;"left"===c?(d.dispatchEvent("expandLeft"),d.klass.add(b.body,"snapjs-expand-left"),d.klass.remove(b.body,"snapjs-expand-right")):(d.dispatchEvent("expandRight"),d.klass.add(b.body,"snapjs-expand-right"),d.klass.remove(b.body,"snapjs-expand-left"),e*=-1),this.action.translate.easeTo(e)},on:function(a,b){return this.eventList[a]=b,this},off:function(a){this.eventList[a]&&(this.eventList[a]=!1)},enable:function(){d.dispatchEvent("enable"),this.action.drag.listen()},disable:function(){d.dispatchEvent("disable"),this.action.drag.stopListening()},settings:function(a){d.extend(this.settings,a)},state:function(){var a,b=this.action.translate.get.matrix(4);return a=b===this.settings.maxPosition?"left":b===this.settings.minPosition?"right":"closed",{state:a,info:this.cache.simpleStates}}}),this[c]=e}).call(this,window,document);

/*! overthrow - An overflow:auto polyfill for responsive design. - v0.7.0 - 2013-11-04
 * Copyright (c) 2013 Scott Jehl, Filament Group, Inc.; Licensed MIT */
!function(a){var b=a.document,c=b.documentElement,d="overthrow-enabled",e="ontouchmove"in b,f="WebkitOverflowScrolling"in c.style||"msOverflowStyle"in c.style||!e&&a.screen.width>800||function(){var b=a.navigator.userAgent,c=b.match(/AppleWebKit\/([0-9]+)/),d=c&&c[1],e=c&&d>=534;return b.match(/Android ([0-9]+)/)&&RegExp.$1>=3&&e||b.match(/ Version\/([0-9]+)/)&&RegExp.$1>=0&&a.blackberry&&e||b.indexOf("PlayBook")>-1&&e&&-1===!b.indexOf("Android 2")||b.match(/Firefox\/([0-9]+)/)&&RegExp.$1>=4||b.match(/wOSBrowser\/([0-9]+)/)&&RegExp.$1>=233&&e||b.match(/NokiaBrowser\/([0-9\.]+)/)&&7.3===parseFloat(RegExp.$1)&&c&&d>=533}();a.overthrow={},a.overthrow.enabledClassName=d,a.overthrow.addClass=function(){-1===c.className.indexOf(a.overthrow.enabledClassName)&&(c.className+=" "+a.overthrow.enabledClassName)},a.overthrow.removeClass=function(){c.className=c.className.replace(a.overthrow.enabledClassName,"")},a.overthrow.set=function(){f&&a.overthrow.addClass()},a.overthrow.canBeFilledWithPoly=e,a.overthrow.forget=function(){a.overthrow.removeClass()},a.overthrow.support=f?"native":"none"}(this),function(a,b,c){if(b!==c){b.easing=function(a,b,c,d){return c*((a=a/d-1)*a*a+1)+b},b.tossing=!1;var d;b.toss=function(a,e){b.intercept();var f,g,h=0,i=a.scrollLeft,j=a.scrollTop,k={top:"+0",left:"+0",duration:50,easing:b.easing,finished:function(){}},l=!1;if(e)for(var m in k)e[m]!==c&&(k[m]=e[m]);return"string"==typeof k.left?(k.left=parseFloat(k.left),f=k.left+i):(f=k.left,k.left=k.left-i),"string"==typeof k.top?(k.top=parseFloat(k.top),g=k.top+j):(g=k.top,k.top=k.top-j),b.tossing=!0,d=setInterval(function(){h++<k.duration?(a.scrollLeft=k.easing(h,i,k.left,k.duration),a.scrollTop=k.easing(h,j,k.top,k.duration)):(f!==a.scrollLeft?a.scrollLeft=f:(l&&k.finished(),l=!0),g!==a.scrollTop?a.scrollTop=g:(l&&k.finished(),l=!0),b.intercept())},1),{top:g,left:f,duration:b.duration,easing:b.easing}},b.intercept=function(){clearInterval(d),b.tossing=!1}}}(this,this.overthrow),function(a,b,c){if(b!==c){b.scrollIndicatorClassName="overthrow";var d=a.document,e=d.documentElement,f="native"===b.support,g=b.canBeFilledWithPoly,h=(b.configure,b.set),i=b.forget,j=b.scrollIndicatorClassName;b.closest=function(a,c){return!c&&a.className&&a.className.indexOf(j)>-1&&a||b.closest(a.parentNode)};var k=!1;b.set=function(){if(h(),!k&&!f&&g){a.overthrow.addClass(),k=!0,b.support="polyfilled",b.forget=function(){i(),k=!1,d.removeEventListener&&d.removeEventListener("touchstart",u,!1)};var j,l,m,n,o=[],p=[],q=function(){o=[],l=null},r=function(){p=[],m=null},s=function(a){n=j.querySelectorAll("textarea, input");for(var b=0,c=n.length;c>b;b++)n[b].style.pointerEvents=a},t=function(a,b){if(d.createEvent){var e,f=(!b||b===c)&&j.parentNode||j.touchchild||j;f!==j&&(e=d.createEvent("HTMLEvents"),e.initEvent("touchend",!0,!0),j.dispatchEvent(e),f.touchchild=j,j=f,f.dispatchEvent(a))}},u=function(a){if(b.intercept&&b.intercept(),q(),r(),j=b.closest(a.target),j&&j!==e&&!(a.touches.length>1)){s("none");var c=a,d=j.scrollTop,f=j.scrollLeft,g=j.offsetHeight,h=j.offsetWidth,i=a.touches[0].pageY,k=a.touches[0].pageX,n=j.scrollHeight,u=j.scrollWidth,v=function(a){var b=d+i-a.touches[0].pageY,e=f+k-a.touches[0].pageX,s=b>=(o.length?o[0]:0),v=e>=(p.length?p[0]:0);b>0&&n-g>b||e>0&&u-h>e?a.preventDefault():t(c),l&&s!==l&&q(),m&&v!==m&&r(),l=s,m=v,j.scrollTop=b,j.scrollLeft=e,o.unshift(b),p.unshift(e),o.length>3&&o.pop(),p.length>3&&p.pop()},w=function(){s("auto"),setTimeout(function(){s("none")},450),j.removeEventListener("touchmove",v,!1),j.removeEventListener("touchend",w,!1)};j.addEventListener("touchmove",v,!1),j.addEventListener("touchend",w,!1)}};d.addEventListener("touchstart",u,!1)}}}}(this,this.overthrow),function(a){a.overthrow.set()}(this);


/*
 * =====================================================================
 * CONSTANTS
 * =====================================================================
 */

/**
 * base path to linker directory, without trailing slash
 */
_BASEPATH = 'https:////linker.cesnet.cz';
// _BASEPATH = '/dokuwiki/lib/tpl/eduid/linker';

/**
 * inlined images data (better performance)
 */
// original logo
_IMG_CESNET_LOGO = 'data:image/png;base64,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';
_IMG_CS_FLAG = 'data:image/gif;base64,R0lGODlhEgANALMAADV4zfLy8s3NzUiCz+cAAJ2dnexvbbfN6+3t7d4AAPj4+N/f3+hfXZIzXKK/5MFZbSH5BAAAAAAALAAAAAASAA0AAARNsMhJKx0H6b26X4I0YEpplkEahMU4OKeiBsgqujCqbqw7ALDZBtFzAQAPg9LAaDJvP0CDQE1Yr4nbcUolYK9abvVrvTXIaNEZTba4LREAOw==';
_IMG_EN_FLAG = 'data:image/gif;base64,R0lGODlhEgANALMAAPv7+wAAPPsAAAUVh52dnfZgVXOQxU50qyhNp6Zxmb/I4qOx1/iPjVRuwXMQKdOi0CH5BAAAAAAALAAAAAASAA0AAARtkMhJKz3roK2cU0OIOIdkMIBiGEBRAE2zvIaELAzaAIIAM7SBzdB4vHi+FwgRkCgA0GgvGlVIHIWedrt1OKnQKdhKGDQGT59YkAg1yywXAgkYtASNd3yxoYcWPAgSBw4hAwEdHwGLAQZvFpAUEQA7';
_IMG_USER = 'data:image/gif;base64,R0lGODlhFgAaAKIAAJ6enktNSnFyccnJyVpbWWVmZMrKyomKiSH5BAAAAAAALAAAAAAWABoAAAOzGLpB9Y+wSaEtNN+bmTuCFWKdQhzDAAgCMByFVBJuWhuDUCrF+oCPlawDOCwAssJrh4oJXsHcLnAb4KzGaeGasg6GOwAXO2VUs2WCQA0AQMCUVkpXDKDauknLwP9SjVt9bgsoXSlGIDSGODoCY100MI84apMAVJdihikPkwcEKlSPX6CbBp+hmpsSqimnBJStVpcBjouSAz2PJHZ9s7Z7rlIMcnxFOCiCvBMFAgfPbc+JEwkAOw==';
_IMG_MOBILE_TOGGLE = 'data:image/gif;base64,R0lGODlhJAAYAIABAOHl6f///yH5BAEAAAEALAAAAAAkABgAAAJPTICpy612oJy0VhCt3hTxz3mGQ5bYeB7m+qAQC6eiGptvCubSrPdzvbrdgCQhiugw0pAMZU/He4J+zIayanUdsYkrt6WS+nDiUKa8kX2zBQA7';




/*
 * =====================================================================
 * MAIN
 * =====================================================================
 */

/**
 * Ensure jQuery is loaded
 */
var $;
if (!window.jQuery) {
    var jq = document.createElement('script'); jq.type = 'text/javascript';
    jq.src = 'http://code.jquery.com/jquery-latest.min.js';
    document.getElementsByTagName('head')[0].appendChild(jq);
}

function onjQueryReady(callback) {
    if (window.jQuery) {
        $ = jQuery;
        callback();
    } else window.setTimeout(function() { onjQueryReady(callback); }, 100);
}

/**
 * Wait for jQuery to load
 */
var cesnet_linker = false;
onjQueryReady(function() {
    $(document).ready(function() {
        placeholder = $('#cesnet_linker_placeholder');
        if (placeholder != null && !cesnet_linker) {
            cesnet_linker = CesnetLinker(placeholder);
        }
    });
});



/*
 * =====================================================================
 * LINKER CLASS
 * =====================================================================
 */

function CesnetLinker(placeholder) {

    var linker = placeholder;
    var mobile_toolbar;
    var content_wrapper;
    var mobile;
    var lang;
    var last_item;
    var snapper;


    /**
     * Data object can be altered here before rendering
     * For example: adding some permanent items should be done here
     */
    var alter_data = function(data, justBrand) {
        var cs_href = linker.attr('data-lang-cs-href');
        var en_href = linker.attr('data-lang-en-href');
        var login_href = linker.attr('data-login-href');

        // login
        if (login_href && !justBrand) {
            var alt = lang == 'en' ? 'user icon' : 'ikonka uživatele';
            var text = lang == 'en' ? 'Login' : 'Přihlášení';
            item = {
                'type': 'link',
                'href': login_href,
                'classes': 'login',
                'label': '<img alt="' + alt + '" src="' + _IMG_USER + '"> ' + text
            }
            data.children.push(item);
        }

        // EN flag
        if (en_href && !justBrand) {
            item = {
                'type': 'link',
                'href': en_href,
                'classes': 'flag-icon spacer',
                'label': '<img alt="english flag" src="' + _IMG_EN_FLAG + '">'
            }
            if (mobile)
                data.children.push(item);
            else
                data.children.unshift(item);
        }

        // CS flag
        if (cs_href && !justBrand) {
            item = {
                'type': 'link',
                'href': cs_href,
                'classes': 'flag-icon',
                'label': '<img alt="česká vlajka" src="' + _IMG_CS_FLAG + '">'
            }
            if (mobile)
                data.children.push(item);
            else
                data.children.unshift(item);
        }

        // Cesnet logo
        item = {
            'type': 'link',
            'href': 'http://www.cesnet.cz',
            'classes': 'logo-cesnet',
            'label': '<img src="' + _IMG_CESNET_LOGO + '" alt="logo sdružení Cesnet">'
        }
        data.children.unshift(item);
    };


    /**
     * Recursively create menu items and return a HTML with complete tree
     */
    var create_item = function(object) {
        var type = object.type;
        var id = object.id ? ' id="' + object.id + '"' : '';
        var classes = object.classes ? ' ' + object.classes : '';
        // var classes = '';
        res = '<div class="item ' + type + classes + '"'+ id +'>';
        // console.log('Processing:', type, object);

        // links
        if (type == 'link') {
            res += '<a class="label" href="' + object.href + '">' + object.label + '</a>';

            // parents
        } else if (type == 'parent') {
            res += '<div class="label">' + object.label + '</div>';
            res += '<div class="children">';
            $.each(object.children, function(key, object) {
                res += create_item(object);
            });
            res += '</div>';

            // headers
        } else if (type == 'header') {
            res += '<div class="label">' + object.label + '</div>';

            // root
        } else if (type == 'root') {
            $.each(object.children, function(type, object) {
                res += create_item(object);
            });

        }

        if (object.hint && type != 'root') res += '<div class="hint">' + object.hint + '</div>';

        res += '</div>';
        return res;
    };


    /**
     * Bind events - open children, hint
     */
    var bind_events = function() {
        if (!mobile) {

            // hide last one, hide tooltip, show new
            get('.item.parent').hover(function(event) {
                event.stopPropagation();
                close_children(last_item);
                if (this != last_item) {
                    $(this).addClass('open');
                    show($(this).children('.children'));
                    last_item = this;
                } else {
                    last_item = null;
                }
            });

            // toggle the oposite classes for window overflowing elements
            var reposition = function() {
                get('.item.parent>.children').each(function() {
                    detectWindowOverflow($(this));
                });
                get('.hint').each(function() {
                    detectWindowOverflow($(this));
                });
            };
            // detect window resize and do callback with 200ms blocking
            var blocking = false;
            $(window).on('resize', function() {
                var timedUnblock = function(callback) {
                    return window.setTimeout(function() {
                        blocking = false;
                        callback();
                    }, 200);
                };
                if (blocking) {
                    clearTimeout(blocking);
                    blocking = timedUnblock(reposition);
                    return;
                }
                blocking = timedUnblock(reposition);
            });
            reposition();

            // show / hide tooltip
            get('.item:not(.parent)').on({
                mouseenter: function(event) {
                    if (!$(this).hasClass('open')) {
                        var hint = $(this).children('.hint')
                        if (hint.length == 0)
                            return;
                        fadeIn(hint, 100);
                    }
                },
                mouseleave: function(event) {
                    var hint = $(this).children('.hint')
                    fadeOut(hint, 100);
                }
            });

        } else {
            $('#cesnet_linker_mobile_toggle').on('touchstart click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                if ( snapper.state().state != 'left')
                    snapper.open('left');
                else {
                    snapper.close();
                }
            });

            // run snapper
            snapper = new Snap({
                element: document.getElementById('cesnet_linker_content_wrapper'),
                disable: 'right',
                addBodyClasses: false
            });

            snapper.on('animated', function() {
                if ( snapper.state().state == 'left')
                    content_wrapper.css('clip', 'rect(0px, ' + (window.innerWidth - linker.outerWidth() - 2) + 'px, auto, 0px)');
            });
            var noClip = function() { content_wrapper.css('clip', '') };
            snapper.on('close', noClip);
            snapper.on('start', noClip);
        }

    };


    /*
     * =====================================================================
     * HELPER FUNCTIONS
     * =====================================================================
     */


    /**
     * returns prefixed selector
     */
    var get = function(selector) {
        return $('#cesnet_linker ' + selector);
    };


    /**
     * add "oposite" class to element when it overflows the screen
     */
    var detectWindowOverflow = function(el) {
        el.removeClass("oposite");
        if (window.innerWidth < parseInt(el.offset().left) + parseInt(el.outerWidth(true)))
            el.addClass("oposite");
    };


    /**
     * close the children
     */
    var close_children = function(item) {
        if (item == null) return;
        $(item).removeClass('open');
        hide($(item).children('.children'));
    };


    /**
     * fade in (fades the visibility)
     */
    var fadeIn = function(el, time, callback) {
        el.fadeTo(time, 1, function() {
            $(this).css("visibility", "visible");
            if (callback) callback();
        });
    };


    /**
     * fade out (fades the visibility)
     */
    var fadeOut = function(el, time, callback) {
        el.fadeTo(time, 0, function() {
            $(this).css("visibility", "hidden");
            if (callback) callback();
        });
    };


    /**
     * show the element (changes its visibility)
     */
    var show = function(el) {
        fadeIn(el, 0);
    };


    /**
     * hide the element (changes its visibility)
     */
    var hide = function(el) {
        fadeOut(el, 0);
    };


    /*
     * =====================================================================
     * CONSTRUCTOR
     * =====================================================================
     */


    /**
     * Create a linker, fetch JSON tree data, populate the linker with items, add CSS
     */
    var __construct = function() {
        linker.hide();

        // setup ENV variables
        mobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Opera Mobi/i.test(navigator.userAgent);

        lang = linker.attr('data-lang')
        if (lang) lang = lang.toLowerCase();
        if (['en', 'cs'].indexOf(lang) < 0) lang = 'cs';

        // add CSS
        head = $('head');
        head.append('<link rel="stylesheet" href="' + _BASEPATH + '/style.css" type="text/css" />');


        if (mobile) {

            // add mobile CSS and JS
            head.append('<link rel="stylesheet" href="' + _BASEPATH + '/mobile.css" type="text/css" />');

            // wrap content and copy body's background - DON'T use jQuery wrap() - possible re-exectution of scripts
            content_wrapper = document.createElement("div");
            content_wrapper.id = "cesnet_linker_content_wrapper";
            while (document.body.firstChild)
                content_wrapper.appendChild(document.body.firstChild);
            document.body.appendChild(content_wrapper);

            content_wrapper = $('#cesnet_linker_content_wrapper');
            content_wrapper.css('background', $('body').css('background'))

            // Setup overthrow
            content_wrapper.addClass("overthrow");
            linker.addClass("overthrow");

            // add extra top bar
            mobile_toolbar_data = {
                'type': 'root',
                'children': [
                    {
                        'type': 'link',
                        'id': 'cesnet_linker_mobile_toggle',
                        'label': '<img alt="ikonka menu" src="' + _IMG_MOBILE_TOGGLE + '">'
                    }
                ]
            }
            alter_data(mobile_toolbar_data);
            content_wrapper.prepend('<div id="cesnet_linker_mobile_toolbar">' + create_item(mobile_toolbar_data) + '<div class="cleaner"></div></div>');
            mobile_toolbar = $('#cesnet_linker_mobile_toolbar');
            mobile_toolbar.hide();
        }

        // change id, add wrapper
        if (mobile) {
            linker.prependTo($('body'));
            linker.addClass("linker-mobile");
        }
        else {
            linker.insertAfter(placeholder);
            linker.addClass("linker-desktop");
        }
        linker.wrap('<div id="cesnet_linker_wrapper"></div>');
        linker.attr('id', 'cesnet_linker');

        // fetch, populate, bind events, add cleaner
        $.getJSON(_BASEPATH + '/data-' + lang + '.json', function(data) {
            alter_data(data, mobile);
            linker.html(create_item(data));
            linker.append('<div class="cleaner"></div>');
            if (mobile)
                mobile_toolbar.show();
            linker.show();
            bind_events();
        });
    };


    __construct();

}