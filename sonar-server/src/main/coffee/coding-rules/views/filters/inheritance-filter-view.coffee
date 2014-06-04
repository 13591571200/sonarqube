define [
  'navigator/filters/choice-filters'
], (
  ChoiceFilters
) ->

  class InheritanceFilterView extends ChoiceFilters.ChoiceFilterView
    tooltip: 'coding_rules.filters.inheritance.inactive'


    initialize: ->
      super
      @qualityProfileFilter = @model.get 'qualityProfileFilter'
      @listenTo @qualityProfileFilter, 'change:value', @onChangeQualityProfile
      @onChangeQualityProfile()


    onChangeQualityProfile: ->
      qualityProfileKey = @qualityProfileFilter.get 'value'
      if _.isArray(qualityProfileKey) && qualityProfileKey.length == 1
        qualityProfile = @options.app.getQualityProfile qualityProfileKey
        parentQualityProfile = @options.app.getQualityProfile qualityProfile.parent
        if parentQualityProfile
          @makeActive()
      else
        @makeInactive()


    makeActive: ->
      @model.set inactive: false, title: ''
      @model.trigger 'change:enabled'
      @$el.removeClass('navigator-filter-inactive').prop 'title', ''


    makeInactive: ->
      @model.set inactive: true, title: t @tooltip
      @model.trigger 'change:enabled'
      @choices.each (model) -> model.set 'checked', false
      @detailsView.updateLists()
      @detailsView.updateValue()
      @$el.addClass('navigator-filter-inactive').prop 'title', t @tooltip


    showDetails: ->
      super unless @$el.is '.navigator-filter-inactive'


    restore: (value) ->
      value = value.split(',') if _.isString(value)
      if @choices && value.length > 0
        @model.set value: value, enabled: true
        @onChangeQualityProfile()
      else
        @clear()
