/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import Config from '../Config';
import { bindProject, getMyProjects } from '../../api';
import { isManualBindingAllowed } from '../../utils';

jest.mock('../../utils', () => ({
  displayMessage: jest.fn(),
  isManualBindingAllowed: jest.fn(() => true)
}));

jest.mock('../../api', () => ({
  bindProject: jest.fn(() => Promise.resolve()),
  displayWSError: jest.fn(),
  getMyProjects: jest.fn(() =>
    Promise.resolve({
      paging: { pageIndex: 1, pageSize: 10, total: 3 },
      projects: [
        { key: 'foo', links: [], name: 'Foo' },
        { key: 'bar', links: [], name: 'Bar' },
        { key: 'baz', links: [], name: 'FooBar' }
      ]
    })
  )
}));

const CONTEXT = { jwt: '' };

beforeEach(() => {
  (bindProject as jest.Mock<any>).mockClear();
  (getMyProjects as jest.Mock<any>).mockClear();
});

it('should display correctly', async () => {
  const wrapper = getWrapper();
  expect(wrapper).toMatchSnapshot();

  await new Promise(setImmediate);
  wrapper.update();
  expect(getMyProjects).toHaveBeenCalled();
  expect(wrapper).toMatchSnapshot();
});

it('should display correctly for auto binding', () => {
  (isManualBindingAllowed as jest.Mock<any>).mockReturnValue(false);
  expect(getWrapper()).toMatchSnapshot();
  (isManualBindingAllowed as jest.Mock<any>).mockReturnValue(true);
});

it('should display correctly for auto binding with already a projectKey', () => {
  (isManualBindingAllowed as jest.Mock<any>).mockReturnValue(false);
  expect(getWrapper()).toMatchSnapshot();
  (isManualBindingAllowed as jest.Mock<any>).mockReturnValue(true);
});

it('should display the authentication component and the display checkbox', async () => {
  (getMyProjects as jest.Mock<any>).mockImplementationOnce(() =>
    Promise.reject({ response: { status: 401 } })
  );
  const wrapper = getWrapper();
  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.find('LoginForm').exists()).toBeTruthy();
  expect(wrapper.find('.settings-form')).toMatchSnapshot();
});

it('should correctly handle select interactions', async () => {
  const wrapper = getWrapper({ projectKey: undefined });
  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper.find('WithAnalyticsContext').prop('isDisabled')).toBeTruthy();

  // Check the select event
  const SelectWrapper = wrapper.find('AtlaskitSelect');
  const projectOption = { label: 'Bar', value: 'bar' };
  (SelectWrapper.prop('onChange') as Function)(projectOption);
  wrapper.update();
  expect(wrapper.state('selectedProject')).toEqual(projectOption);
  expect(wrapper.find('WithAnalyticsContext').prop('isDisabled')).toBeFalsy();
});

it('should correctly bind a project', async () => {
  const updateProjectKey = jest.fn();
  const wrapper = getWrapper({ projectKey: undefined, updateProjectKey });
  await new Promise(setImmediate);
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('WithAnalyticsContext').prop('isDisabled')).toBeTruthy();

  (wrapper.find('AtlaskitSelect').prop('onChange') as Function)({ label: 'Baz', value: 'baz' });
  wrapper.update();
  expect(wrapper.find('WithAnalyticsContext').prop('isDisabled')).toBeFalsy();
  wrapper.find('form').simulate('submit', { preventDefault: () => {} });
  expect(wrapper.find('WithAnalyticsContext').prop('isDisabled')).toBeTruthy();
  expect(bindProject).toHaveBeenCalledWith({ ...CONTEXT, projectKey: 'baz' });

  await new Promise(setImmediate);
  expect(updateProjectKey).toHaveBeenCalledWith('baz');
});

function getWrapper(props = {}) {
  return shallow(
    <Config
      context={CONTEXT}
      disabled={false}
      projectKey="foo"
      updateDisabled={jest.fn()}
      updateProjectKey={jest.fn()}
      {...props}
    />
  );
}
