var pg;
(function (pg) {
    // Terrible element resize hack methods
    function resizeTriggerListener(e) {
        var win = e.target;
        var trigger = win.__resizeOwner__;
        trigger.__resizeListeners__.forEach(function (fn) {
            fn.call(trigger, e);
        });
    }
    function addElementResizeListener(element, listener) {
        function onLoad(e) {
            obj.onload = null;
            obj.contentDocument.defaultView.__resizeOwner__ = element;
            obj.contentDocument.defaultView.addEventListener('resize', resizeTriggerListener);
        }
        if (!element.__resizeListeners__) {
            element.__resizeListeners__ = [];
            if (getComputedStyle(element).position == 'static')
                element.style.position = 'relative';
            var obj = element.__resizeTrigger__ = document.createElement('object');
            obj.setAttribute('style', 'display: block; position: absolute; top: 0; left: 0; height: 100%; width: 100%; overflow: hidden; pointer-events: none; z-index: -1;');
            obj.onload = onLoad;
            obj.type = 'text/html';
            obj.data = 'about:blank';
            element.appendChild(obj);
        }
        element.__resizeListeners__.push(listener);
    }
    pg.addElementResizeListener = addElementResizeListener;
    function removeElementResizeListener(element, listener) {
        element.__resizeListeners__.splice(element.__resizeListeners__.indexOf(listener), 1);
        if (!element.__resizeListeners__.length) {
            element.__resizeTrigger__.contentDocument.defaultView.removeEventListener('resize', resizeTriggerListener);
            element.removeChild(element.__resizeTrigger__);
            element.__resizeTrigger__ = null;
            element.__resizeListeners__ = null;
        }
    }
    pg.removeElementResizeListener = removeElementResizeListener;
    // end horrible hack stuff
})(pg || (pg = {}));
(function (pg) {
    var ui;
    (function (ui) {
        var ScrollController = (function () {
            function ScrollController(container, opts) {
                this.WheelVelocity = 30;
                this.MinThumbHeight = 20;
                this.MaxThumbHeight = 0;
                this.ScrollPos = 0;
                this.MinPos = 0;
                this.ThumbHeight = 0;
                this.ThumbScale = 0;
                this.WheelVelocity = opts.wheelVelocity != null ? opts.wheelVelocity : this.WheelVelocity;
                this.MinThumbHeight = opts.minThumbHeight != null ? opts.minThumbHeight : this.MinThumbHeight;
                this.MaxThumbHeight = opts.maxThumbHeight != null ? opts.maxThumbHeight : this.MaxThumbHeight;
                this.Container = container;
                this.Content = container.children[0];
                this.TopShadow = document.createElement('div');
                this.TopShadow.style.position = "absolute";
                this.TopShadow.style.width = "100%";
                this.TopShadow.style.top = "0px";
                this.TopShadow.style.pointerEvents = "none";
                this.TopShadow.classList.add("scrollTopShadow");
                this.Container.appendChild(this.TopShadow);
                this.BottomShadow = document.createElement('div');
                this.BottomShadow.style.position = "absolute";
                this.BottomShadow.style.width = "100%";
                this.BottomShadow.style.bottom = "0px";
                this.TopShadow.style.pointerEvents = "none";
                this.BottomShadow.classList.add("scrollBottomShadow");
                this.Container.appendChild(this.BottomShadow);
                this.ScrollThumb = document.createElement('div');
                this.ScrollThumb.style.position = "absolute";
                this.ScrollThumb.classList.add("scrollThumb");
                this.Container.appendChild(this.ScrollThumb);
                this.setupEvents();
                this.calcDimensions();
            }
            ScrollController.prototype.redraw = function () {
                var topOpacity = this.ScrollPos / -100.0;
                if (topOpacity < 0)
                    topOpacity = 0;
                else if (topOpacity > 1.0)
                    topOpacity = 1.0;
                this.TopShadow.style.opacity = String(topOpacity);
                var bottomOpacity = (this.ScrollPos - this.MinPos) / 100.0;
                if (bottomOpacity < 0)
                    bottomOpacity = 0;
                else if (bottomOpacity > 1.0)
                    bottomOpacity = 1.0;
                this.BottomShadow.style.opacity = String(bottomOpacity);
                if (this.ThumbScale != 0) {
                    var thumbPos = this.ScrollPos * this.ThumbScale;
                    this.ScrollThumb.style.top = Math.round(thumbPos) + "px";
                }
            };
            ScrollController.prototype.calcDimensions = function () {
                this.ContainerHeight = this.Container.offsetHeight;
                this.ContentHeight = this.Content.offsetHeight;
                this.MinPos = this.ContainerHeight - this.ContentHeight;
                if (this.MinPos > 0)
                    this.MinPos = 0;
                this.ThumbHeight = this.ContainerHeight * this.ContainerHeight / this.ContentHeight;
                if (this.ThumbHeight < this.MinThumbHeight)
                    this.ThumbHeight = this.MinThumbHeight;
                else if (this.MaxThumbHeight > 0 && this.ThumbHeight > this.MaxThumbHeight)
                    this.ThumbHeight = this.MaxThumbHeight;
                if (this.ContentHeight == 0 || this.ContainerHeight > this.ContentHeight) {
                    this.ScrollThumb.style.display = "none";
                    this.ThumbScale = 0;
                }
                else {
                    this.ScrollThumb.style.display = "block";
                    this.ScrollThumb.style.height = this.ThumbHeight + "px";
                    this.ThumbScale = (this.ContainerHeight - this.ThumbHeight) / this.MinPos;
                }
                this.setScroll(this.ScrollPos);
            };
            ScrollController.prototype.setScroll = function (pos) {
                if (pos > 0)
                    pos = 0;
                else if (pos < this.MinPos)
                    pos = this.MinPos;
                this.ScrollPos = Math.round(pos);
                this.Content.style.top = this.ScrollPos + "px";
                this.redraw();
            };
            ScrollController.prototype.scroll = function (delta) {
                this.setScroll(this.ScrollPos - delta);
            };
            ScrollController.prototype.setupEvents = function () {
                var inst = this;
                this.Container.addEventListener("wheel", onWheel);
                this.ScrollThumb.addEventListener("mousedown", onThumbMouseDown);
                pg.addElementResizeListener(this.Container, onContainerResized);
                pg.addElementResizeListener(this.Content, onContentResized);
                var startMouseY = 0;
                var startScrollPos = 0;
                function onWheel(ev) {
                    var delta = ev.deltaY > 0 ? inst.WheelVelocity : -inst.WheelVelocity;
                    inst.scroll(delta);
                    ev.preventDefault();
                }
                function onContainerResized(ev) {
                    inst.calcDimensions();
                }
                function onContentResized(ev) {
                    inst.calcDimensions();
                }
                function onThumbMouseMove(ev) {
                    var offset = ev.clientY - startMouseY;
                    if (inst.ThumbScale != 0) {
                        var scrollOffset = offset / inst.ThumbScale;
                        inst.setScroll(startScrollPos + scrollOffset);
                    }
                    ev.preventDefault();
                }
                function onThumbMouseUp(ev) {
                    document.removeEventListener("mousemove", onThumbMouseMove);
                    document.removeEventListener("mouseup", onThumbMouseUp);
                }
                function onThumbMouseDown(ev) {
                    startMouseY = ev.clientY;
                    startScrollPos = inst.ScrollPos;
                    document.addEventListener("mousemove", onThumbMouseMove);
                    document.addEventListener("mouseup", onThumbMouseUp);
                    ev.preventDefault();
                }
            };
            return ScrollController;
        }());
        function makeScrollPanel(container, opts) {
            if (!opts)
                opts = {};
            container.style.overflow = "hidden";
            return new ScrollController(container, opts);
        }
        ui.makeScrollPanel = makeScrollPanel;
    })(ui = pg.ui || (pg.ui = {}));
})(pg || (pg = {}));
(function (pg) {
    var library;
    (function (library) {
        var PreviewPane = null;
        var Library = null;
        var LibraryPalette = null;
        var CurrentlySelected = null;
        function initLibrary() {
            Library = document.getElementById("library");
            if (Library == null)
                return;
            Library.style.display = "none";
            PreviewPane = document.createElement("div");
            PreviewPane.classList.add("pgbare");
            PreviewPane.style.left = "0px";
            PreviewPane.style.right = "200px";
            PreviewPane.style.height = "100%";
            document.body.appendChild(PreviewPane);
            if (window.location.href.substring(0, 5) != "file:")
                return;
            var hash = window.location.hash.substring(1);
            var libPath = hash.split("/");
            if (libPath.length == 0 || libPath[0] != "library")
                return;
            LibraryPalette = document.createElement("div");
            LibraryPalette.classList.add("pgbare");
            LibraryPalette.style.width = "200px";
            LibraryPalette.style.right = "0px";
            LibraryPalette.style.height = "100%";
            LibraryPalette.style.backgroundColor = "#dddddd";
            LibraryPalette.innerHTML =
                "<div style='text-align:center;padding-top:10px;'>Library</div>";
            document.body.appendChild(LibraryPalette);
            var scrollContainer = document.createElement("div");
            scrollContainer.classList.add("pgbare");
            scrollContainer.style.width = "100%";
            scrollContainer.style.top = "50px";
            scrollContainer.style.bottom = "0px";
            LibraryPalette.appendChild(scrollContainer);
            var scrollPanel = document.createElement("div");
            scrollContainer.appendChild(scrollPanel);
            var list = document.createElement("ul");
            list.id = "libraryList";
            scrollPanel.appendChild(list);
            var libItems = Library.children;
            for (var i = 0; i < libItems.length; i++) {
                var libItem = libItems[i];
                if (libItem.id == null)
                    libItem.id = "Library Item " + i;
                var li = document.createElement("li");
                li.id = "li_" + libItem.id;
                li.innerHTML = libItem.id;
                li.addEventListener("click", libraryItemClicked);
                list.appendChild(li);
            }
            pg.ui.makeScrollPanel(scrollContainer, {});
            libPath.shift();
            if (libPath.length > 0) {
                var node = libPath.shift();
                showLibraryItem(node);
            }
        }
        function libraryItemClicked(ev) {
            var item = ev.currentTarget;
            var itemId = item.innerHTML;
            showLibraryItem(itemId);
        }
        function showLibraryItem(itemId) {
            if (Library == null)
                return;
            var item = document.getElementById(itemId);
            if (item == null)
                return;
            while (PreviewPane.lastChild != null) {
                PreviewPane.removeChild(PreviewPane.lastChild);
            }
            var cloned = item.cloneNode(true);
            PreviewPane.appendChild(cloned);
            if (CurrentlySelected) {
                var prevLI = document.getElementById("li_" + CurrentlySelected);
                prevLI.classList.remove("librarySelected");
            }
            CurrentlySelected = itemId;
            var newLI = document.getElementById("li_" + CurrentlySelected);
            newLI.classList.add("librarySelected");
            history.pushState(null, "Library", "#library/" + itemId);
        }
        function getItem(itemId) {
            var item = document.getElementById(itemId);
            if (item == null)
                return;
            return item.cloneNode(true);
        }
        library.getItem = getItem;
        initLibrary();
    })(library = pg.library || (pg.library = {}));
})(pg || (pg = {}));
