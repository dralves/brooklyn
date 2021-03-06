/**
 * Jasmine test specs for Application Template and Template rendering. We test for HTML structure,
 * ID's, classes that we use, etc.
 *
 * Tests go like this:
 * - we test that all templates are on the page
 * - we test that the templates contain all the divs, buttons with the right id's and classes
 * - we test that rendering produces the right stuff
 */
define([
    "underscore", "jquery", "model/application", "view/home",
    "text!tpl/home/location-option.html", "text!tpl/home/location-entry.html", "text!tpl/home/entry.html",
    "text!tpl/home/key-value.html"
], function (_, $, Application, HomeView, LocationOptionHtml, LocationEntryHtml, EntryHtml, KeyValueHtml) {

    Backbone.View.prototype.callPeriodically = function (callback, interval) {
        if (!this._periodicFunctions) {
            this._periodicFunctions = []
        }
        this._periodicFunctions.push(setInterval(callback, interval))
    }

    describe('view/home-spec', function () {

        describe('tpl/home templates', function () {

            it('tpl/home/key-value.html must contain i.remove and two span inside a li', function () {
                var $template = $(KeyValueHtml)
                expect($template.is('li')).toBeTruthy()
                expect($template.find('span').length).toEqual(2)
                expect($template.find('i.remove').length).toEqual(1)
            })
            it('tpl/home/entry.html must contain i.remove and a span inside a li', function () {
                var $template = $(EntryHtml)
                expect($template.is('li')).toBeTruthy()
                expect($template.find('span').length).toEqual(1)
                expect($template.find('i.remove').length).toEqual(1)
            })

            it('tpl/home/location-option.html must have <option/> and some <span/>', function () {
                var $locationOptionTemplate = $(LocationOptionHtml)
                expect($locationOptionTemplate.is('option')).toBeTruthy()
                expect($locationOptionTemplate.find('span').length).toEqual(2)
            })

            it('tpl/home/location-entry.html must have a <li/> and 3 spans', function () {
                var $locationEntryTemplate = $(LocationEntryHtml)
                expect($locationEntryTemplate.is('li')).toBeTruthy()
                expect($locationEntryTemplate.find('span').length).toEqual(3)
                expect($locationEntryTemplate.find('i.remove').length).toEqual(1)
            })
        })

        describe('view/home HomePageView', function () {
            var view,
                apps = new Application.Collection
            apps.url = "fixtures/application-list.json"
            apps.fetch({async:false})

            beforeEach(function () {
                view = new HomeView({
                    collection:apps
                }).render()
            })

            afterEach(function () {
                view.close()
            })

            it('must be div#application-resource', function () {
                expect(view.$('div#application-resource').length).toEqual(1)
            })

            it('div#application-resource must have div#new-application-resource and div#applications', function () {
                expect(view.$('div#new-application-resource').length).toEqual(1)
                expect(view.$('div#applications').length).toEqual(1)
            })

            it('div#new-application-resource must have button#add-new-application-resource', function () {
                expect(view.$('div button#add-new-application').length).toEqual(1)
            })

            it('div#applications must have table with tbody#applications-table-body', function () {
                expect(view.$('div#applications table').length).toEqual(1)
                expect(view.$('div#applications tbody#applications-table-body').length).toEqual(1)
            })

            it('must have div#modal-container', function () {
                expect(view.$('div#modal-container').length).toEqual(1)
            })
        })

        describe('view/home ApplicationEntryView rendering', function () {
            var model = new Application.Model({
                status:'STARTING',
                spec:new Application.Spec({
                    name:'sample',
                    entities:[
                        { name:'entity-01',
                            type:'org.apache.TomcatServer'}
                    ],
                    locations:['/some/where/over/the/rainbow']
                })
            })
            var view = new HomeView.AppEntryView({
                model:model
            }).render()

            it('must have 3 td tags', function () {
                expect(view.$('td').length).toEqual(3)
            })

            it('must have a td with button.delete', function () {
                expect(view.$('button.delete').length).toEqual(1)
                expect(view.$('button.delete').parent().is('td')).toEqual(true)
                expect(view.$("button.delete").attr("id")).toBe(model.cid)
            })
        })
    })
})