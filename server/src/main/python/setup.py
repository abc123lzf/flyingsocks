#!/usr/bin/python
# -*- coding: UTF-8 -*-

# 服务器设置

import platform

class Properties(object):

    def __init__(self, file):
        self.fileName = file
        self.properties = {}

    def __getDict(self, str_name, dict_name, value):

        if str_name.find('.') > 0:
            k = str_name.split('.')[0]
            dict_name.setdefault(k, {})
            return self.__getDict(str_name[len(k) + 1:], dict_name[k], value)
        else:
            dict_name[str_name] = value
            return

    def get_properties(self):
        try:
            pro_file = open(self.fileName, 'Ur')
            for line in pro_file.readlines():
                line = line.strip().replace('\n', '')
                if line.find("#") != -1:
                    line = line[0:line.find('#')]
                if line.find('=') > 0:
                    strings = line.split('=')
                    strings[1] = line[len(strings[0]) + 1:]
                    self.__getDict(strings[0].strip(), self.properties, strings[1].strip())
        except Exception as e:
            raise e
        else:
            pro_file.close()
        return self.properties


def open_config_file():
    location_properties = Properties("../config/config.properties")
    sys_type = platform.system()
    if sys_type == "Windows":
        return ""


if __name__ == '__main__':
    open_config_file()
