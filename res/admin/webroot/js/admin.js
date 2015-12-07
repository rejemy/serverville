var sv;
(function (sv) {
    function init() {
        var hash = window.location.hash.substring(1);
        var stateStack = hash.split("/");
    }
    init();
})(sv || (sv = {}));
