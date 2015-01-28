define [
  'backbone.marionette'
  'templates/coding-rules'
], (
  Marionette
  Templates
) ->

  class CodingRulesStatusView extends Marionette.ItemView
    template: Templates['coding-rules-actions']


    collectionEvents:
      'all': 'render'


    ui:
      orderChoices: '.navigator-actions-order-choices'
      bulkChange: '.navigator-actions-bulk'


    events:
      'click .navigator-actions-order': 'toggleOrderChoices'
      'click .navigator-actions-order-choices li': 'sort'
      'click @ui.bulkChange': 'bulkChange'


    onRender: ->
      unless @collection.sorting.sortText
        @collection.sorting.sortText = @$("[data-sort=#{@collection.sorting.sort}]:first").text()
        @$('.navigator-actions-ordered-by').text @collection.sorting.sortText


    toggleOrderChoices: (e) ->
      e.stopPropagation()
      @ui.orderChoices.toggleClass 'open'
      if @ui.orderChoices.is '.open'
        jQuery('body').on 'click.coding_rules_actions', =>
          @ui.orderChoices.removeClass 'open'


    sort: (e) ->
      e.stopPropagation()
      @ui.orderChoices.removeClass 'open'
      jQuery('body').off 'click.coding_rules_actions'
      el = jQuery(e.currentTarget)
      sort = el.data 'sort'
      asc = el.data 'asc'
      if sort? && asc?
        @collection.sorting = sort: sort, sortText: el.text(), asc: asc
        @options.app.fetchFirstPage()


    bulkChange: (e) ->
      e.stopPropagation()
      @options.app.codingRulesBulkChangeDropdownView.toggle()


    serializeData: ->
      _.extend super,
        canWrite: @options.app.canWrite
        paging: @collection.paging
        sorting: @collection.sorting
